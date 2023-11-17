import {definePlugin} from "@halo-dev/console-shared";
import HomeView from "./views/BackupSetting.vue";
import type {BackupTab} from "@halo-dev/console-shared/dist/states/backup";
import {markRaw} from "vue";

export default definePlugin({
    components: {},
    routes: [],
    extensionPoints: {
        "backup:tabs:create": (): BackupTab[] => {
            return [
                {
                    id: "settings",
                    label: "设置",
                    component: markRaw(HomeView),
                    permissions: ["system:migrations:manage"],
                    showOperations: true,
                }
            ];
        }
    },
});
