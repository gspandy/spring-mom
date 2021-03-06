/**
 * Copyright 2010-2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.excilys.spring.mom.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.spring.mom.annotation.MOMAttributeEncoding;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Concrete class implemented {@link MOMResponseParser MOMResponseParser}.
 * <p/>
 * This implementation parse data as a JSON string. Each annotated method parameters will try to be mapped with the
 * according value of the JSON oject.
 * <p/>
 * According the client receive the JSON string '{var1:value1,var2:value2,var3:value3}' on a subscribed topic and the
 * mapping method signature is the following :
 * <p/>
 * <code>@MOMMapping(topic = "topic", consumes = MOMMappingConsum.JSON)
 * public void received(@MOMAttribute("var1") String param1, @MOMAttribute("var3") String param2) {...}</code>
 * <p/>
 * Then the method invoke will be like this : <code>received("value1", "value3");</code>
 * 
 * @author dvilleneuve
 * @see MOMResponseParser
 */
public class MOMResponseJSONAttributesParser implements MOMResponseParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(MOMResponseJSONAttributesParser.class);

	private final ParameterInfo[] bindAttributes;

	public MOMResponseJSONAttributesParser(ParameterInfo[] bindAttributes) {
		this.bindAttributes = bindAttributes;
	}

	@Override
	public Object[] parse(byte[] data) {
		List<Object> results = new ArrayList<Object>();
		Map<String, Object> jsonMap;

		try {
			jsonMap = ObjectMapperSingleton.INSTANCE.getMapper().readValue(data, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			LOGGER.warn("Unable to parse the json string : {}", new String(data), e);
			return null;
		}

		// For each annotated parameter, try to get back the json value according to the key
		for (ParameterInfo bindAttribute : bindAttributes) {
			Object attributeValue = jsonMap.get(bindAttribute.getName());

			// Decode a base64 encoded string
			if (bindAttribute.getEncoding() == MOMAttributeEncoding.BASE64) {
				if (attributeValue instanceof String) {
					attributeValue = Base64.decodeBase64((String) attributeValue);
				} else if (attributeValue instanceof byte[]) {
					attributeValue = Base64.decodeBase64((byte[]) attributeValue);
				}
			}

			if (attributeValue != null) {
				results.add(attributeValue);
			}
		}

		return results.toArray();
	}
}
