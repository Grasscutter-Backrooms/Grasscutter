package emu.grasscutter.server.http.dispatch;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.Grasscutter.ServerRunMode;
import emu.grasscutter.net.proto.QueryCurrRegionHttpRspOuterClass.*;
import emu.grasscutter.net.proto.RegionInfoOuterClass;
import emu.grasscutter.net.proto.RegionInfoOuterClass.RegionInfo;
import emu.grasscutter.net.proto.RegionSimpleInfoOuterClass.RegionSimpleInfo;
import emu.grasscutter.server.event.dispatch.QueryAllRegionsEvent;
import emu.grasscutter.server.event.dispatch.QueryCurrentRegionEvent;
import emu.grasscutter.server.http.Router;
import emu.grasscutter.server.http.objects.QueryCurRegionRspJson;
import emu.grasscutter.utils.Crypto;
import emu.grasscutter.utils.FileUtils;
import emu.grasscutter.utils.Utils;
import express.Express;
import express.http.Request;
import express.http.Response;
import io.javalin.Javalin;

import javax.crypto.Cipher;
import java.io.File;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static emu.grasscutter.Configuration.*;
import static emu.grasscutter.net.proto.QueryRegionListHttpRspOuterClass.*;

/**
 * Handles requests related to region queries.
 */
public final class RegionHandler implements Router {
    private static final Map<String, RegionData> regions = new ConcurrentHashMap<>();
    private static String regionListResponse;

    public RegionHandler() {
        try { // Read & initialize region data.
            this.initialize();
        } catch (Exception exception) {
            Grasscutter.getLogger().error("Failed to initialize region data.", exception);
        }
    }

    /**
     * Configures region data according to configuration.
     */
    private void initialize() {
        String dispatchDomain = "http" + (HTTP_ENCRYPTION.useInRouting ? "s" : "") + "://"
                + lr(HTTP_INFO.accessAddress, HTTP_INFO.bindAddress) + ":"
                + lr(HTTP_INFO.accessPort, HTTP_INFO.bindPort);

        // Create regions.
        List<RegionSimpleInfo> servers = new ArrayList<>();
        List<String> usedNames = new ArrayList<>(); // List to check for potential naming conflicts.

        var configuredRegions = new ArrayList<>(List.of(DISPATCH_INFO.regions));
        if(SERVER.runMode != ServerRunMode.HYBRID && configuredRegions.size() == 0) {
            Grasscutter.getLogger().error("[Dispatch] There are no game servers available. Exiting due to unplayable state.");
            System.exit(1);
        } else if (configuredRegions.size() == 0)
            configuredRegions.add(new Region("os_usa", DISPATCH_INFO.defaultName,
                    lr(GAME_INFO.accessAddress, GAME_INFO.bindAddress),
                    lr(GAME_INFO.accessPort, GAME_INFO.bindPort)));

        configuredRegions.forEach(region -> {
            if (usedNames.contains(region.Name)) {
                Grasscutter.getLogger().error("Region name already in use.");
                return;
            }

            // Create a region identifier.
            var identifier = RegionSimpleInfo.newBuilder()
                    .setName(region.Name).setTitle(region.Title).setType("DEV_PUBLIC")
                    .setDispatchUrl(dispatchDomain + "/query_cur_region/" + region.Name)
                    .build();
            usedNames.add(region.Name); servers.add(identifier);

            // Create a region info object.
            var regionInfo = RegionInfo.newBuilder()
                    .setGateserverIp(region.Ip).setGateserverPort(region.Port)
                    //.setSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED))
                    .setSecretKey(ByteString.copyFrom(new byte[]{0}))
                    .build();
            // Create an updated region query.
            var updatedQuery = QueryCurrRegionHttpRsp.newBuilder().setRegionInfo(regionInfo).build();
            regions.put(region.Name, new RegionData(updatedQuery, Utils.base64Encode(updatedQuery.toByteString().toByteArray())));
        });

        // Create a config object.
        byte[] customConfig = "{\"sdkenv\":\"2\",\"checkdevice\":\"false\",\"loadPatch\":\"false\",\"showexception\":\"false\",\"regionConfig\":\"pm|fk|add\",\"downloadMode\":\"0\"}".getBytes();
        Crypto.xor(customConfig, Crypto.DISPATCH_KEY, false); // XOR the config with the key.

        // Create an updated region list.
        QueryRegionListHttpRsp updatedRegionList = QueryRegionListHttpRsp.newBuilder()
                .addAllRegionList(servers)
                .setClientSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED))
                .setClientCustomConfigEncrypted(ByteString.copyFrom(customConfig))
                .setEnableLoginPc(true).build();

        // Set the region list response.
        regionListResponse = Utils.base64Encode(updatedRegionList.toByteString().toByteArray());
    }

    @Override public void applyRoutes(Express express, Javalin handle) {
        express.get("/query_region_list", RegionHandler::queryRegionList);
        express.get("/query_cur_region/:region", RegionHandler::queryCurrentRegion );
    }

    /**
     * @route /query_region_list
     */
    private static void queryRegionList(Request request, Response response) {
        // Invoke event.
        QueryAllRegionsEvent event = new QueryAllRegionsEvent(regionListResponse); event.call();
        // Respond with event result.
        response.send(event.getRegionList());

        // Log to console.
        Grasscutter.getLogger().info(String.format("[Dispatch] Client %s request: query_region_list", request.ip()));
    }

    /**
     * @route /query_cur_region/:region
     */
    private static void queryCurrentRegion(Request request, Response response) {
        // Get region to query.
        String regionName = request.params("region");
        String versionName = request.query("version");
        var region = regions.get(regionName);

        // Get region data.
        String regionData = "CAESGE5vdCBGb3VuZCB2ZXJzaW9uIGNvbmZpZw==";
        if (request.query().values().size() > 0) {
            if(region != null)
                regionData = region.getBase64();
        }

        if( versionName.contains("2.7.5") || versionName.contains("2.8.")) {
            try {
                var key = new String(FileUtils.readResource("/cert/" + (versionName.contains("OSCB") ? "OSCB.pem" : "OSCN.pem") ), Charset.defaultCharset());

                String publicKeyPEM = key
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replaceAll(System.lineSeparator(), "")
                        .replace("-----END PUBLIC KEY-----", "");

                byte[] encoded = Utils.base64Decode(publicKeyPEM);

                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

                PrivateKey pk = keyFactory.generatePrivate(keySpec);

                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, pk);

                QueryCurrentRegionEvent event = new QueryCurrentRegionEvent(regionData); event.call();
                var regionInfo = event.getRegionInfo();

                String encodedRsp = Utils.base64Encode(cipher.doFinal(Utils.base64Decode(regionInfo)));
                var rsp = new QueryCurRegionRspJson();

                rsp.content = encodedRsp;
                rsp.sign = "ZnVja195b3VfbWh5";

                response.send(rsp);
                //var rsp = "{\"content\":\"rr9YPwJ0RHy8ZBaV9yMb1ZV0b8XGN3nEYdns/mjc4cke1pxcYt9nfqgDfNKVqod0zBHc/SBWS7smIdnvr/Y2zmyCwU4NGtK7oMPzgEfURalLbSj+k4fhI1GkH3pki4kTtiISPj2RuJAN5KLZ1ANZhIxHVOb2nbHED9gzkIhwkk5GaTIE4H6OIE+3eorFiMKwX7e1jsnCSGWZ3V/3NszzSP+j0LHwyeyLm9rghRgRiVbIlhYheNNwPLeQ/EA5iRHuU4uxLLdb/jl47iNgB24uS/BfIPWDKeCubcYJJ8xfPE2fFuqZ5495vPmJOfX3tnxrBFuNQ3oUSGp1wdh9CalIlw==\",\"sign\":\"gK2Q0wgTjqtnuffdFLyC6TYqwMhrdWRy3DaeQPQquFEVPOqSU9E7WoYhKa/jbHhQJVqpBzo+Kmi8Mn+0MZu8qhlhWw0lTCtr8/DYX13qqwYyfSlXSdkJ+lfCqtykMeJmmVM4QzazL4mjFIIQ3dlBg7OaMooBcX29BO3eucPIiL1BRv9Q4BhPMlYfFLReKqDSJZzvLOl8WAEsEPuEPF26zKJ2EFOvFmeTgLqqk8vvc7k3EnIKbGZlMeNfx2pjGeTpmsRafGTLwpJWlGBHsPSfrpTENLLtxh6uFIDtjVqnIy8QQ3IXcmvFpgdwAYlJdvD31qSWet2Pzbe3wQATQelyNA==\"}";
            }
            catch(Exception ignored) {

            }
        }
        else {
            // Invoke event.
            QueryCurrentRegionEvent event = new QueryCurrentRegionEvent(regionData); event.call();
            // Respond with event result.
            response.send(event.getRegionInfo());
        }
        // Log to console.
        Grasscutter.getLogger().info(String.format("Client %s request: query_cur_region/%s", request.ip(), regionName));
    }

    /**
     * Region data container.
     */
    public static class RegionData {
        private final QueryCurrRegionHttpRsp regionQuery;
        private final String base64;

        public RegionData(QueryCurrRegionHttpRsp prq, String b64) {
            this.regionQuery = prq;
            this.base64 = b64;
        }

        public QueryCurrRegionHttpRsp getRegionQuery() {
            return this.regionQuery;
        }

        public String getBase64() {
            return this.base64;
        }
    }

    /**
     * Gets the current region query.
     * @return A {@link QueryCurrRegionHttpRsp} object.
     */
    public static QueryCurrRegionHttpRsp getCurrentRegion() {
        return SERVER.runMode == ServerRunMode.HYBRID ? regions.get("os_usa").getRegionQuery() : null;
    }
}
