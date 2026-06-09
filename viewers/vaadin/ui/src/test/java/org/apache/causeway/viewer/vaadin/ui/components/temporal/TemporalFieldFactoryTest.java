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
package org.apache.causeway.viewer.vaadin.ui.components.temporal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalFieldFactoryTest {

    @Test
    void handlesOnlyLocalDateAndLocalDateTime() {
        assertTrue(TemporalFieldFactory.handles(LocalDate.class));
        assertTrue(TemporalFieldFactory.handles(LocalDateTime.class));
        assertFalse(TemporalFieldFactory.handles(String.class));
        assertFalse(TemporalFieldFactory.handles(null));
    }

    @Test
    void dateField_initializesFromCurrentValue_andWritesBack() {
        var today = LocalDate.of(2026, 6, 9);
        var written = new ArrayList<LocalDate>();

        var picker = TemporalFieldFactory.createDateField("When", today, false, written::add);

        assertEquals("When", picker.getLabel());
        assertEquals(today, picker.getValue());

        var tomorrow = today.plusDays(1);
        picker.setValue(tomorrow);
        assertEquals(java.util.List.of(tomorrow), written);
    }

    @Test
    void dateField_readOnly_neitherWritable_norWiresWriteBack() {
        var written = new ArrayList<LocalDate>();
        var picker = TemporalFieldFactory.createDateField("When", null, true, written::add);

        assertNull(picker.getValue());
        assertTrue(picker.isReadOnly());
    }

    @Test
    void dateTimeField_initializesFromCurrentValue_andWritesBack() {
        var now = LocalDateTime.of(2026, 6, 9, 12, 30);
        var written = new ArrayList<LocalDateTime>();

        var picker = TemporalFieldFactory.createDateTimeField("When", now, false, written::add);

        assertEquals(now, picker.getValue());

        var later = now.plusHours(1);
        picker.setValue(later);
        assertEquals(java.util.List.of(later), written);
    }
}
