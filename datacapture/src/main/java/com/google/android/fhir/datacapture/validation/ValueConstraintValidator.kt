/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.datacapture.validation

import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

internal open class ValueConstraintValidator(
  val url: String,
  val predicate: (Int, Int) -> Boolean,
  val messageGenerator: (allowedValue: String) -> String
) : ConstraintValidator {
  override fun validate(
    questionnaireItem: Questionnaire.QuestionnaireItemComponent,
    questionnaireResponseItem: QuestionnaireResponse.QuestionnaireResponseItemComponent
  ): ConstraintValidator.ConstraintValidationResult {
    if (questionnaireItem.hasExtension(url)) {
      val extension = questionnaireItem.getExtensionByUrl(url)
      val answer = questionnaireResponseItem.answer[0]
      when {
        extension.value.fhirType().equals("integer") && answer.hasValueIntegerType() -> {
          val answeredValue = answer.valueIntegerType.value
          if (predicate(answeredValue, extension.value.primitiveValue().toInt())) {
            return ConstraintValidator.ConstraintValidationResult(
              false,
              messageGenerator(extension.value.primitiveValue().toInt().toString())
            )
          }
        }
      }
    }
    return ConstraintValidator.ConstraintValidationResult(true, null)
  }
}