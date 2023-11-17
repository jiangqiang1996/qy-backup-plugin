import qs from "qs";
import axios, {AxiosRequestConfig, AxiosResponse} from "axios";
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
  (response: AxiosResponse) => {
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

const processResult = <T = any>(response: AxiosResponse<any>): T => {
  const result = response.data
  if (result?.code === undefined) {
    return result;
  } else {
    return result.data;
  }
}
declare type GVK = {
  group: string;
  version: string;
  kind: string;
  plural: string;
  singular: string;
}
declare type ExtensionOperator = {
  group: string;
  version: string;
  kind: string;
  plural: string;
  singular: string;
}

const service = {
  async get(gvk: GVK, name: string): Promise<AxiosResponse<ExtensionOperator>> {
    return axiosInstance.request<any, AxiosResponse<ExtensionOperator>, any>({
      url: `/apis/${gvk.group}/${gvk.version}/${gvk.plural}`,
      params: {
        name
      }
    });
  },
  async post(gvk: GVK, name: string): Promise<AxiosResponse<ExtensionOperator>> {
    return axiosInstance.request<any, AxiosResponse<ExtensionOperator>, any>({
      url: `/apis/${gvk.group}/${gvk.version}/${gvk.plural}`,
      params: {
        name
      }
    });
  },
  async delete(gvk: GVK, name: string): Promise<AxiosResponse<ExtensionOperator>> {
    return axiosInstance.request<any, AxiosResponse<ExtensionOperator>, any>({
      url: `/apis/${gvk.group}/${gvk.version}/${gvk.plural}`,
      params: {
        name
      }
    });
  },
  async put(gvk: GVK, name: string): Promise<AxiosResponse<ExtensionOperator>> {
    return axiosInstance.request<any, AxiosResponse<ExtensionOperator>, any>({
      url: `/apis/${gvk.group}/${gvk.version}/${gvk.plural}`,
      params: {
        name
      }
    });
  },
  async list(gvk: GVK, name: string): Promise<AxiosResponse<ExtensionOperator>> {
    return axiosInstance.request<any, AxiosResponse<ExtensionOperator>, any>({
      url: `/apis/${gvk.group}/${gvk.version}/${gvk.plural}`,
      params: {
        name
      }
    });
  },
}

axiosInstance.defaults.headers.common["X-Requested-With"] = "XMLHttpRequest";
// TODO 使用halo console 中的axios https://github.com/halo-dev/halo/issues/3979
export default service;
