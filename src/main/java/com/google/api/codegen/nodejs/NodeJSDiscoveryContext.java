/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.nodejs;

import com.google.api.client.util.DateTime;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;
import com.google.api.Service;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

/**
 * A DiscoveryContext specialized for NodeJS.
 */
public class NodeJSDiscoveryContext extends DiscoveryContext implements NodeJSContext {

  public NodeJSDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
  }

  private static final ImmutableMap<Field.Kind, String> DEFAULT_VALUES =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_UNKNOWN, "{}")
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "0")
          .put(Field.Kind.TYPE_UINT32, "0")
          .put(Field.Kind.TYPE_INT64, "''")
          .put(Field.Kind.TYPE_UINT64, "''")
          .put(Field.Kind.TYPE_FLOAT, "0.0")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .build();

  /**
   * Generates placeholder assignment (to end of line) for field of type based on field kind and,
   * for explicitly-formatted strings, format type in {@link ApiaryConfig#stringFormat}.
   */
  public String typeDefaultValue(Type type, Field field, Method method) {
    // used to handle inconsistency in translate v2 API.
    if (getApi().getName().equals("translate")
        && getApi().getVersion().equals("v2")
        && (method.getName().equals("language.detections.list")
            || method.getName().equals("language.translations.list"))
        && field.getName().equals("q")) {
      return "\"\",";
    }

    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      return isMapField(type, field.getName()) ? "{}," : "[],";
    }
    if (DEFAULT_VALUES.containsKey(field.getKind())) {
      return DEFAULT_VALUES.get(field.getKind()) + ",";
    }
    if (field.getKind() == Field.Kind.TYPE_STRING) {
      String stringFormat = getApiaryConfig().getStringFormat(type.getName(), field.getName());
      if (stringFormat != null) {
        switch (stringFormat) {
          case "byte":
            return "\"\","
                + "  // base64-encoded string of bytes: see http://tools.ietf.org/html/rfc4648";
          case "date":
            // TODO(tcoffee): does new DateTime(new Date(0L)).toStringRfc3339() work?
            return "\"1969-12-31\"," + "  // \"YYYY-MM-DD\"";
          case "date-time":
            return "\""
                + new DateTime(0L).toStringRfc3339()
                + "\","
                + "  // \"YYYY-MM-DDThh:mm:ss.fffZ\" (UTC)";
          default:
            // fall through
        }
      }
      return "\"\",";
    }
    return "null,";
  }

  private static final ImmutableMap<String, String> MAP_PARAM_NAME =
      ImmutableMap.<String, String>builder().put("resource", "resource_").build();

  public String mapParamName(String p) {
    return MAP_PARAM_NAME.containsKey(p) ? MAP_PARAM_NAME.get(p) : p;
  }
}
