package com.youshangdache.driver.service.impl;

import com.youshangdache.driver.config.TencentCloudProperties;
import com.youshangdache.driver.service.CiService;
import com.youshangdache.model.vo.order.TextAuditingVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ciModel.auditing.*;
import com.qcloud.cos.region.Region;
import jakarta.annotation.Resource;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CiServiceImpl implements CiService {
    @Resource
    private TencentCloudProperties tencentCloudProperties;

    @Override
    public TextAuditingVo textAuditing(String content) {
        if (!StringUtils.hasText(content)) {
            TextAuditingVo textAuditingVo = new TextAuditingVo();
            textAuditingVo.setResult("0");
            return textAuditingVo;
        }
        COSClient cosClient = this.getCOSClient();
        TextAuditingRequest request = new TextAuditingRequest();
        request.setBucketName(tencentCloudProperties.getBucketPrivate());
        byte[] encoder = Base64.encodeBase64(content.getBytes());
        String contentBase64 = new String(encoder);
        request.getInput().setContent(contentBase64);
        request.getConf().setDetectType("all");

        TextAuditingResponse response = cosClient.createAuditingTextJobs(request);
        AuditingJobsDetail detail = response.getJobsDetail();
        TextAuditingVo textAuditingVo = new TextAuditingVo();
        if ("Success".equals(detail.getState())) {
            String result = detail.getResult();
            StringBuffer keywords = new StringBuffer();
            List<SectionInfo> sectionInfoList = detail.getSectionList();
            for (SectionInfo info : sectionInfoList) {
                String pornInfoKeyword = info.getPornInfo().getKeywords();
                String illegalInfoKeyword = info.getIllegalInfo().getKeywords();
                String abuseInfoKeyword = info.getAbuseInfo().getKeywords();
                if (pornInfoKeyword.length() > 0) {
                    keywords.append(pornInfoKeyword).append(",");
                }
                if (illegalInfoKeyword.length() > 0) {
                    keywords.append(illegalInfoKeyword).append(",");
                }
                if (abuseInfoKeyword.length() > 0) {
                    keywords.append(abuseInfoKeyword).append(",");
                }
            }
            textAuditingVo.setResult(result);
            textAuditingVo.setKeywords(keywords.toString());
        }
        return textAuditingVo;
    }

    @Override
    public Boolean imageAuditing(String path) {
        ImageAuditingRequest request = new ImageAuditingRequest();
        request.setBucketName(tencentCloudProperties.getBucketPrivate());
        request.setObjectKey(path);
        COSClient client = this.getCOSClient();
        ImageAuditingResponse response = client.imageAuditing(request);
        client.shutdown();
        if (!response.getPornInfo().getHitFlag().equals("0") ||
                !response.getAdsInfo().getHitFlag().equals("0") ||
                !response.getTerroristInfo().getHitFlag().equals("0") ||
                !response.getPoliticsInfo().getHitFlag().equals("0")
        ) {
            return false;
        }
        return true;
    }

    public COSClient getCOSClient() {
        String secretId = tencentCloudProperties.getSecretId();
        String secretKey = tencentCloudProperties.getSecretKey();
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region region = new Region(tencentCloudProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        clientConfig.setHttpProtocol(HttpProtocol.https);
        COSClient cosClient = new COSClient(cred, clientConfig);
        return cosClient;
    }
}
