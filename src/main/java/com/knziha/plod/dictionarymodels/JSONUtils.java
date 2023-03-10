package com.knziha.plod.dictionarymodels;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;

public class JSONUtils {
	public static JSONArray toJSONArray(Object value) {
		if (value instanceof JSONArray) {
			return (JSONArray)value;
		} else if (value instanceof List) {
			return new JSONArray((List)value);
		} else {
			return value instanceof String ? (JSONArray) JSON.parse((String)value) : (JSONArray)JSONObject.toJSON(value);
		}
	}
	
	public static JSONObject toJSONObject(Object value) {
		if (value instanceof JSONObject) {
			return (JSONObject)value;
		} else if (value instanceof Map) {
			return new JSONObject((Map)value);
		} else {
			return value instanceof String ? JSON.parseObject((String)value) : (JSONObject)JSONObject.toJSON(value);
		}
	}
	
	/**
	 * 合并JSON对象，用source覆盖target，返回覆盖后的JSON对象，
	 *
	 * @param source JSONObject
	 * @param target JSONObject
	 * @return JSONObject
	 */
	public static JSONObject jsonMerge(JSONObject source, JSONObject target) {
		// 覆盖目标JSON为空，直接返回覆盖源
		if (target == null) {
			return source;
		}
		
		for (String key : source.keySet()) {
			Object value = source.get(key);
			if (!target.containsKey(key)) {
				target.put(key, value);
			} else {
				if (value instanceof JSONObject) {
					JSONObject valueJson = (JSONObject) value;
					JSONObject targetValue = jsonMerge(valueJson, target.getJSONObject(key));
					target.put(key, targetValue);
				} else if (value instanceof JSONArray) {
					JSONArray valueArray = (JSONArray) value;
					for (int i = 0; i < valueArray.size(); i++) {
						JSONObject obj = (JSONObject) valueArray.get(i);
						JSONObject targetValue = jsonMerge(obj, (JSONObject) target.getJSONArray(key).get(i));
						target.getJSONArray(key).set(i, targetValue);
					}
				} else {
					target.put(key, value);
				}
			}
		}
		return target;
	}
}
