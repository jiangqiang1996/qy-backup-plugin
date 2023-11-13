package run.halo.starter.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(
    group = "www.jiangqiang.top",//命名规则类似包名，最好全局唯一
    version = "v1",//版本号，建议字母开头
    kind = "WxConfig",//给这一系列增删改查接口分组，建议使用当前实体类的类名
    singular = "wxConfig",//当前实体的单数形式
    plural = "wxConfig")//当前实体的复数表现形式
@Schema(description = "微信配置实体")
public class WxConfig extends AbstractExtension {
    @Schema(description = "微信key")
    private String key;
    @Schema(description = "微信secret")
    private String secret;

}
