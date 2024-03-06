package com.xiaoyu.kexueone.utils;

import cn.hutool.core.lang.Dict;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.yaml.YamlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * 获取application.yml配置的工具
 *
 * @Author weibo
 * @Date 2024/2/29 8:45
 **/
public class ConfigUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);

    private static Dict config = null;

    static {
        try (InputStream in = ConfigUtil.class.getClassLoader().getResourceAsStream("application.yml")) {
            config = YamlUtil.load(in, Dict.class);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException("configUtil load error");
        }
    }

    public static String getStrConfig(String key) {
        return config.getStr(key);
    }

    public static Integer getIntConfig(String key) {
        return config.getInt(key);
    }

    public static <T> T getBeanConfig(String keyPrefix, Class<T> t) {
        Object bean = config.getBean(keyPrefix);
        if (null == bean) {
            return null;
        }
        return JSONUtil.toBean(JSONUtil.toJsonStr(bean), t);
    }
}
