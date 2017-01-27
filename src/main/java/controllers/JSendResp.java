package controllers;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by ganitzsh on 1/27/17.
 */
public class JSendResp {
    @JsonIgnore
    private Integer status;
    private Object data;
    private Boolean success = false;

    public JSendResp(Integer status, Object data) {
        this.status = status;
        this.data = data;
        if (status >= 200 && status < 300) {
            this.success = true;
        }
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
