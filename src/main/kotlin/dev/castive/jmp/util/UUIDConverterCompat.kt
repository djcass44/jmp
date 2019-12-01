/*
 *    Copyright 2019 Django Cass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dev.castive.jmp.util

import dev.dcas.util.extend.uuid
import java.util.UUID
import javax.persistence.AttributeConverter

class UUIDConverterCompat: AttributeConverter<UUID, String> {
	override fun convertToDatabaseColumn(attribute: UUID): String {
		return attribute.toString()
	}

	override fun convertToEntityAttribute(dbData: String): UUID {
		return dbData.uuid() ?: error("Cannot convert to UUID: $dbData")
	}
}
