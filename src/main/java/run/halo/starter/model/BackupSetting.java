package run.halo.starter.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(
    group = "www.jiangqiang.top",//命名规则类似包名，最好全局唯一
    version = "v1",//版本号，建议字母开头
    kind = "BackupSetting",//给这一系列增删改查接口分组，建议使用当前实体类的类名
    singular = "BackupSetting",//当前实体的单数形式
    plural = "BackupSettings")//当前实体的复数表现形式
@Schema(description = "备份同步")
public class BackupSetting extends AbstractExtension {

    @Schema(description = "执行周期", requiredMode = REQUIRED)
    private Long period = 1L;
    @Schema(description = "时间单位", requiredMode = REQUIRED)
    private TimeUnit timeUnit = TimeUnit.DAYS;
    @Schema(description = "有效时长", requiredMode = REQUIRED)
    private Long effectiveDuration = 1L;
    @Schema(description = "有效时长单位", requiredMode = REQUIRED)
    private TemporalUnit effectiveDurationUnit = ChronoUnit.WEEKS;
}
