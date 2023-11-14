package run.halo.starter.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.core.extension.attachment.Attachment;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(
    group = "www.jiangqiang.top",//命名规则类似包名，最好全局唯一
    version = "v1",//版本号，建议字母开头
    kind = "BackupSynchronization",//给这一系列增删改查接口分组，建议使用当前实体类的类名
    singular = "BackupSynchronization",//当前实体的单数形式
    plural = "BackupSynchronization")//当前实体的复数表现形式
@Schema(description = "备份同步")
public class BackupSynchronization extends AbstractExtension {
    @Schema(requiredMode = REQUIRED)
    private Attachment.AttachmentSpec spec;
    @Schema(description = "同步状态", requiredMode = REQUIRED)
    private SynchronizationStatus status;

}
