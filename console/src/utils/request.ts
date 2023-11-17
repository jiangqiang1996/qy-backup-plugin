import qs from "qs";
import axios, {type AxiosResponse} from "axios";
import {Toast} from "@halo-dev/components";


const baseURL = import.meta.env.VITE_API_URL;
const axiosInstance = axios.create({
  baseURL,
  timeout: 50000,
  headers: {'Content-Type': 'application/json'},
  paramsSerializer: {
    serialize: function (params: Record<string, any>) {
      return qs.stringify(params, {allowDots: true});
    }
  },
  withCredentials: true,
});

// 非200状态码就弹窗
axiosInstance.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const errorResponse = error.response;
    if (!errorResponse) {
      return Promise.reject(error);
    }
    const {status} = errorResponse;
    if (status !== 200) {
      Toast.error("status: " + status);
    }
    return Promise.reject(error);
  }
);

/**
 * GVK，生成接口路径
 */
export declare type GVK = {
  group: string;
  version: string;
  kind: string;
  plural: string;
  singular: string;
}
/**
 * 每一个实体的metadata字段的类型
 */
declare type Metadata = {
  name: string;
  version?: number;
  annotations?: Map<string, string>
  creationTimestamp?: Date;
  deletionTimestamp?: Date;
  finalizers?: Array<string>;
  generateName?: string;
  labels?: Map<string, string>
};
declare type BaseExtensionRequest = {
  metadata: Metadata,
  [key: string]: any
};
/**
 * 插件实体公共字段
 */
declare type BaseExtension = BaseExtensionRequest & {
  apiVersion: string;
  kind: string;
}
/**
 * 列表查询条件参数
 */
declare type PageQuery = {
  fieldSelector?: string;
  labelSelector?: string;
  page?: string,
  size?: string,
  sort?: string,
}
/**
 * 列表查询结果
 */
declare type PageRes<T extends BaseExtension> = {
  first: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  items: Array<T>;
  last: boolean;
  page: number,
  size: number,
  total: number,
  totalPages: number,
}
const service = {
  async create<T extends BaseExtension>(gvk: GVK, data: BaseExtensionRequest): Promise<AxiosResponse<T>> {
    data.apiVersion = `${gvk.group}/${gvk.version}`;
    data.kind = gvk.kind;
    return axiosInstance.post<any, AxiosResponse<T>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}`, data);
  },
  async get<T extends BaseExtension>(gvk: GVK, name: string): Promise<AxiosResponse<T>> {
    return axiosInstance.get<any, AxiosResponse<T>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}/${name}`);
  },
  async delete(gvk: GVK, name: string): Promise<AxiosResponse<any>> {
    return axiosInstance.delete<any, AxiosResponse<any>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}/${name}`);
  },
  async update<T extends BaseExtension>(gvk: GVK, name: string, data: BaseExtensionRequest): Promise<AxiosResponse<T>> {
    data.apiVersion = `${gvk.group}/${gvk.version}`;
    data.kind = gvk.kind;
    data.metadata.name = name;
    return axiosInstance.put<any, AxiosResponse<T>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}/${name}`, data);
  },
  async list<T extends BaseExtension>(gvk: GVK, query: PageQuery): Promise<AxiosResponse<PageRes<T>>> {
    return axiosInstance.get<any, AxiosResponse<PageRes<T>>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}`, {
      params: query
    });
  },
}

axiosInstance.defaults.headers.common["X-Requested-With"] = "XMLHttpRequest";
export default service;
