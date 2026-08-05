package com.youshangdache.service.driver;

import com.youshangdache.model.vo.order.TextAuditingVo;

public interface CiService {

    Boolean imageAuditing(String path);

    TextAuditingVo textAuditing(String content);
}
