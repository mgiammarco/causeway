/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.causeway.viewer.vaadin.ui.i18n;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.vaadin.flow.i18n.I18NProvider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default {@link I18NProvider} for the Vaadin viewer, backed by a
 * {@code translations/messages[_locale].properties} resource bundle. Registered
 * only when the application does not supply its own {@code I18NProvider}, so an
 * app can override it. Providing locale-specific bundles enables translation of
 * any component text looked up by key; unknown keys fall back to the key itself.
 */
@Component
@ConditionalOnMissingBean(I18NProvider.class)
public class VaadinI18nProviderVaa implements I18NProvider {

    private static final long serialVersionUID = 1L;
    private static final String BUNDLE = "translations.messages";
    private static final List<Locale> LOCALES = List.of(Locale.ENGLISH, Locale.ITALIAN);

    @Override
    public List<Locale> getProvidedLocales() {
        return LOCALES;
    }

    @Override
    public String getTranslation(final String key, final Locale locale, final Object... params) {
        try {
            var bundle = ResourceBundle.getBundle(BUNDLE, locale != null ? locale : Locale.ENGLISH);
            if (bundle.containsKey(key)) {
                var value = bundle.getString(key);
                return params.length > 0 ? MessageFormat.format(value, params) : value;
            }
        } catch (MissingResourceException ignore) {
            // no bundle for this locale; fall through to returning the key
        }
        return key;
    }
}
