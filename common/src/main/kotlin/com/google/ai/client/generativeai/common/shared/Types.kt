/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.client.generativeai.common.shared

import com.google.ai.client.generativeai.common.util.FirstOrdinalSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object HarmCategorySerializer :
  KSerializer<HarmCategory> by FirstOrdinalSerializer(HarmCategory::class)

@Serializable(HarmCategorySerializer::class)
enum class HarmCategory {
  UNKNOWN,
  @SerialName("HARM_CATEGORY_HARASSMENT") HARASSMENT,
  @SerialName("HARM_CATEGORY_HATE_SPEECH") HATE_SPEECH,
  @SerialName("HARM_CATEGORY_SEXUALLY_EXPLICIT") SEXUALLY_EXPLICIT,
  @SerialName("HARM_CATEGORY_DANGEROUS_CONTENT") DANGEROUS_CONTENT
}

typealias Base64 = String

@ExperimentalSerializationApi
@Serializable
data class Content(@EncodeDefault val role: String? = "user", val parts: List<Part>)

@Serializable(PartSerializer::class) sealed interface Part

@Serializable data class TextPart(val text: String) : Part

@Serializable data class BlobPart(@SerialName("inline_data") val inlineData: Blob) : Part

@Serializable data class FileDataPart(@SerialName("file_data") val fileData: FileData) : Part

@Serializable
data class FileData(
  @SerialName("mime_type") val mimeType: String,
  @SerialName("file_uri") val fileUri: String
)

@Serializable
data class Blob(
  @SerialName("mime_type") val mimeType: String,
  val data: Base64,
)

@Serializable
data class SafetySetting(val category: HarmCategory, val threshold: HarmBlockThreshold)

@Serializable
enum class HarmBlockThreshold {
  @SerialName("HARM_BLOCK_THRESHOLD_UNSPECIFIED") UNSPECIFIED,
  BLOCK_LOW_AND_ABOVE,
  BLOCK_MEDIUM_AND_ABOVE,
  BLOCK_ONLY_HIGH,
  BLOCK_NONE,
}

object PartSerializer : JsonContentPolymorphicSerializer<Part>(Part::class) {
  override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Part> {
    val jsonObject = element.jsonObject
    return when {
      "text" in jsonObject -> TextPart.serializer()
      "inline_data" in jsonObject -> BlobPart.serializer()
      "file_data" in jsonObject -> FileDataPart.serializer()
      else -> throw SerializationException("Unknown Part type")
    }
  }
}