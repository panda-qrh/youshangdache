package com.qrh.youshangdache.model.form.rules;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class FeeRuleRequestForm {

    @Schema(description = "代驾里程")
    private BigDecimal distance;

    @Schema(description = "代驾时间")
    private Date startTime;

    @Schema(description = "等候分钟")
    private Integer waitMinute;

    public FeeRuleRequestForm(){}

    public FeeRuleRequestForm(Builder builder){
        this.distance = builder.distance;
        this.startTime = builder.startTime;
        this.waitMinute = builder.waitMinute;
    }

    public static Builder builder() {
        return new Builder();
    }


      public static class Builder {
        @Schema(description = "代驾里程")
        private BigDecimal distance;

        @Schema(description = "代驾时间")
        private Date startTime;

        @Schema(description = "等候分钟")
        private Integer waitMinute;

        public Builder(){}

        public Builder distance(BigDecimal distance){
            this.distance = distance;
            return this;
        }

        public Builder startTime(Date startTime){
            this.startTime = startTime;
            return this;
        }
        public Builder waitMinute(Integer waitMinute){
            this.waitMinute = waitMinute;
            return this;
        }

        public FeeRuleRequestForm build(){
            return new FeeRuleRequestForm(this);
        }



    }
}
