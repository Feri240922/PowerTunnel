/*
 * This file is part of PowerTunnel.
 *
 * PowerTunnel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerTunnel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerTunnel.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.krlvm.powertunnel.preferences;

import io.github.krlvm.powertunnel.exceptions.PreferenceParseException;
import io.github.krlvm.powertunnel.i18n.I18NBundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class PreferenceParser {

    public static final String FILE = "preferences.json";

    public static List<PreferenceGroup> parsePreferences(String source, String json, I18NBundle bundle) throws PreferenceParseException {
        final JSONArray array;
        try {
            array = new JSONArray(json);
        } catch (JSONException ex) {
            throw new PreferenceParseException(source, "Malformed preferences JSON", ex);
        }

        final List<PreferenceGroup> groups = new ArrayList<>();
        final List<Preference> ungrouped = new ArrayList<>();
        for (Object object : array) {
            if (!(object instanceof JSONObject))
                throw new PreferenceParseException(source, "Malformed preferences structure");
            final JSONObject jso = ((JSONObject) object);
            if(jso.has(PreferencesGroupSchemaFields.ID)) {
                if (!jso.has(PreferencesGroupSchemaFields.PREFERENCES))
                    throw new PreferenceParseException(
                            source,
                            "One of preference groups is incomplete (missing 'preferences')"
                    );
                final Object jsoList = jso.get(PreferencesGroupSchemaFields.PREFERENCES);
                if(!(jsoList instanceof JSONArray)) throw new PreferenceParseException(source, "Preferences list should be array");

                final String title = jso.has(PreferencesGroupSchemaFields.TITLE) ? jso.getString(PreferencesGroupSchemaFields.TITLE) :
                        bundle.get(jso.getString(PreferencesGroupSchemaFields.ID));
                final String description = jso.has(PreferencesGroupSchemaFields.DESCRIPTION) ? jso.getString(PreferencesGroupSchemaFields.DESCRIPTION) :
                        bundle.get(jso.getString(PreferencesGroupSchemaFields.ID) + ".desc");
                groups.add(new PreferenceGroup(
                        title,
                        description,
                        parsePreferenceList(source, ((JSONArray) jsoList), bundle)
                ));
            } else {
                ungrouped.add(parsePreference(source, jso, bundle));
            }
        }
        if(!ungrouped.isEmpty()) {
            groups.add(0, new PreferenceGroup(null, null, ungrouped));
        }
        return groups;
    }

    public static List<Preference> parsePreferenceList(String source, JSONArray array, I18NBundle bundle) throws PreferenceParseException {
        final List<Preference> preferences = new ArrayList<>();
        for (Object object : array) {
            if (!(object instanceof JSONObject))
                throw new PreferenceParseException(source, "Malformed preferences list structure");
            preferences.add(parsePreference(source, ((JSONObject) object), bundle));
        }
        return preferences;
    }

    public static Preference parsePreference(String source, JSONObject jso, I18NBundle bundle) throws PreferenceParseException {
        if (!jso.has(PreferencesSchemaFields.KEY) || !jso.has(PreferencesSchemaFields.TYPE))
            throw new PreferenceParseException(source, "One of preferences is incomplete (missing 'key' and (or) 'type')");

        final String rawType = jso.getString(PreferencesSchemaFields.TYPE);
        final PreferenceType type;
        try {
            type = PreferenceType.valueOf(rawType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new PreferenceParseException(source, "Unsupported preference type: '" + rawType + '"', ex);
        }
        final String key = jso.getString(PreferencesSchemaFields.KEY);

        Map<String, String> items = null;
        if(type == PreferenceType.SELECT) {
            if(!jso.has(PreferencesSchemaFields.ITEMS)) {
                throw new PreferenceParseException(source, "Preference with type 'select' doesn't have items list");
            }
            items = new LinkedHashMap<>();

            final Object jsoItemsList = jso.get(PreferencesSchemaFields.ITEMS);
            if(!(jsoItemsList instanceof JSONArray)) throw new PreferenceParseException(source, "Select Preference item list should be array");
            final JSONArray itemsArray = ((JSONArray) jsoItemsList);
            for (Object o : itemsArray) {
                if(!(o instanceof JSONObject))
                    throw new PreferenceParseException(source, "Malformed select preference items structure");
                final JSONObject ijo = ((JSONObject) o);
                if(!ijo.has(PreferencesSelectItemSchemaFields.KEY))
                    throw new PreferenceParseException(source, "One of select preferences items is incomplete (missing 'key')");

                final String ikey = ijo.getString(PreferencesSelectItemSchemaFields.KEY);
                final String name = ijo.has(PreferencesSelectItemSchemaFields.NAME) ? ijo.getString(PreferencesSelectItemSchemaFields.NAME) :
                        bundle.get(key + ".item." + ikey);
                items.put(ikey, name);
            }
        }

        final Object defaultValue = getObjectOrNull(jso, PreferencesSchemaFields.DEFAULT_VALUE);
        final Object dependencyValue = getObjectOrNull(jso, PreferencesSchemaFields.DEPENDENCY_VALUE);

        final String title = jso.has(PreferencesSchemaFields.TITLE) ? jso.getString(PreferencesSchemaFields.TITLE) :
                bundle.get(jso.getString(PreferencesSchemaFields.KEY));
        final String description = jso.has(PreferencesSchemaFields.DESCRIPTION) ? jso.getString(PreferencesSchemaFields.DESCRIPTION) :
                bundle.get(jso.getString(PreferencesSchemaFields.KEY) + ".desc");

        return new Preference(
                key,
                title,
                description,
                defaultValue != null ? defaultValue.toString() : type.getDefaultValue(),
                type,
                getStringOrNull(jso, PreferencesSchemaFields.DEPENDENCY),
                dependencyValue != null ? dependencyValue.toString() : "true",
                items
        );
    }

    private static String getStringOrNull(JSONObject jso, String key) {
        return jso.has(key) ? jso.getString(key) : null;
    }

    private static Object getObjectOrNull(JSONObject jso, String key) {
        return jso.has(key) ? jso.get(key) : null;
    }


    static class PreferencesSchemaFields {
        static final String KEY = "key";
        static final String TITLE = "title";
        static final String DESCRIPTION = "description";
        static final String TYPE = "type";
        static final String DEFAULT_VALUE = "defaultValue";

        static final String DEPENDENCY = "dependency";
        static final String DEPENDENCY_VALUE = "dependencyValue";

        static final String ITEMS = "items";
    }

    static class PreferencesGroupSchemaFields {
        static final String ID = "group";
        static final String TITLE = "title";
        static final String DESCRIPTION = "description";
        static final String PREFERENCES = "preferences";
    }

    static class PreferencesSelectItemSchemaFields {
        static final String KEY = "key";
        static final String NAME = "name";
    }
}
