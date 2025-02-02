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

package com.google.android.fhir.datacapture

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class QuestionnaireViewModelTest {
  private lateinit var state: SavedStateHandle

  @Before
  fun setUp() {
    state = SavedStateHandle()
  }

  @Test
  fun stateHasNoQuestionnaireResponse_shouldCopyQuestionnaireId() {
    val questionnaire = Questionnaire().setId("a-questionnaire")
    val serializedQuestionnaire = printer.encodeResourceToString(questionnaire)
    state.set(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE, serializedQuestionnaire)
    val viewModel = QuestionnaireViewModel(state)

    assertResourceEquals(
      viewModel.getQuestionnaireResponse(),
      QuestionnaireResponse().apply { setQuestionnaire("Questionnaire/a-questionnaire") }
    )
  }

  @Test
  fun stateHasNoQuestionnaireResponse_shouldCopyQuestion() {
    val questionnaire =
      Questionnaire().apply {
        id = "a-questionnaire"
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "a-link-id"
            text = "Yes or no?"
            type = Questionnaire.QuestionnaireItemType.BOOLEAN
          }
        )
      }
    val serializedQuestionnaire = printer.encodeResourceToString(questionnaire)
    state.set(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE, serializedQuestionnaire)
    val viewModel = QuestionnaireViewModel(state)

    assertResourceEquals(
      viewModel.getQuestionnaireResponse(),
      QuestionnaireResponse().apply {
        setQuestionnaire("Questionnaire/a-questionnaire")
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply { linkId = "a-link-id" }
        )
      }
    )
  }

  @Test
  fun stateHasNoQuestionnaireResponse_shouldCopyQuestionnaireStructure() {
    val questionnaire =
      Questionnaire().apply {
        id = "a-questionnaire"
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "a-link-id"
            text = "Basic questions"
            type = Questionnaire.QuestionnaireItemType.GROUP
            addItem(
              Questionnaire.QuestionnaireItemComponent().apply {
                linkId = "another-link-id"
                text = "Name?"
                type = Questionnaire.QuestionnaireItemType.STRING
              }
            )
          }
        )
      }
    val serializedQuestionnaire = printer.encodeResourceToString(questionnaire)
    state.set(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE, serializedQuestionnaire)
    val viewModel = QuestionnaireViewModel(state)

    assertResourceEquals(
      viewModel.getQuestionnaireResponse(),
      QuestionnaireResponse().apply {
        setQuestionnaire("Questionnaire/a-questionnaire")
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "a-link-id"
            addItem(
              QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                linkId = "another-link-id"
              }
            )
          }
        )
      }
    )
  }

  @Test
  fun stateHasQuestionnaireResponse_nestedItemsWithinGroupItems_shouldNotThrowException() { // ktlint-disable max-line-length
    val questionnaire =
      Questionnaire().apply {
        id = "a-questionnaire"
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "a-link-id"
            text = "Basic questions"
            type = Questionnaire.QuestionnaireItemType.GROUP
            addItem(
              Questionnaire.QuestionnaireItemComponent().apply {
                linkId = "another-link-id"
                text = "Is this true?"
                type = Questionnaire.QuestionnaireItemType.BOOLEAN
                addItem(
                  Questionnaire.QuestionnaireItemComponent().apply {
                    linkId = "yet-another-link-id"
                    text = "Name?"
                    type = Questionnaire.QuestionnaireItemType.STRING
                  }
                )
              }
            )
          }
        )
      }
    val serializedQuestionnaire = printer.encodeResourceToString(questionnaire)
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "a-questionnaire-reponse"
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "a-link-id"
            text = "Basic questions"
            addItem(
              QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                linkId = "another-link-id"
                text = "Is this true?"
                addAnswer(
                  QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    value = BooleanType(true)
                    addItem(
                      QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                        linkId = "yet-another-link-id"
                        text = "Name?"
                        addAnswer(
                          QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value = StringType("a-name")
                          }
                        )
                      }
                    )
                  }
                )
              }
            )
          }
        )
      }
    val serializedQuestionnaireResponse = printer.encodeResourceToString(questionnaireResponse)
    state.set(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE, serializedQuestionnaire)
    state.set(
      QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE_RESPONSE,
      serializedQuestionnaireResponse
    )

    QuestionnaireViewModel(state)
  }

  @Test
  fun stateHasQuestionnaireResponse_nestedItemsWithinNonGroupItems_shouldNotThrowException() {
    val questionnaire =
      Questionnaire().apply {
        id = "a-questionnaire"
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "a-link-id"
            text = "Is this true?"
            type = Questionnaire.QuestionnaireItemType.BOOLEAN
            addItem(
              Questionnaire.QuestionnaireItemComponent().apply {
                linkId = "another-link-id"
                text = "Name?"
                type = Questionnaire.QuestionnaireItemType.STRING
              }
            )
          }
        )
      }
    val serializedQuestionnaire = printer.encodeResourceToString(questionnaire)
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "a-questionnaire"
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "a-link-id"
            text = "Is this true?"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = BooleanType(true)
                addItem(
                  QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                    linkId = "another-link-id"
                    text = "Name?"
                    addAnswer(
                      QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                        value = StringType("a-name")
                      }
                    )
                  }
                )
              }
            )
          }
        )
      }
    val serializedQuestionnaireResponse = printer.encodeResourceToString(questionnaireResponse)
    state.set(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE, serializedQuestionnaire)
    state.set(
      QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE_RESPONSE,
      serializedQuestionnaireResponse
    )

    QuestionnaireViewModel(state)
  }

  @Test
  fun stateHasQuestionnaireResponse_wrongLinkId_shouldThrowError() {
    val questionnaire =
      Questionnaire().apply {
        id = "a-questionnaire"
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "a-link-id"
            text = "Basic question"
            type = Questionnaire.QuestionnaireItemType.BOOLEAN
          }
        )
      }
    val serializedQuestionniare = printer.encodeResourceToString(questionnaire)
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "a-questionnaire-response"
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "a-different-link-id"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = BooleanType(true)
              }
            )
          }
        )
      }
    val serializedQuestionniareResponse = printer.encodeResourceToString(questionnaireResponse)
    state.set(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE, serializedQuestionniare)
    state.set(
      QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE_RESPONSE,
      serializedQuestionniareResponse
    )

    val errorMessage =
      assertFailsWith<IllegalArgumentException> { QuestionnaireViewModel(state) }.localizedMessage

    assertThat(errorMessage)
      .isEqualTo(
        "Mismatching linkIds for questionnaire item a-link-id and " +
          "questionnaire response item a-different-link-id"
      )
  }

  @Test
  fun stateHasQuestionnaireResponse_lessItemsInQuestionnaireResponse_shouldThrowError() {
    val questionnaire =
      Questionnaire().apply {
        id = "a-questionnaire"
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "a-link-id"
            text = "Basic question"
            type = Questionnaire.QuestionnaireItemType.BOOLEAN
          }
        )
      }
    val serializedQuestionniare = printer.encodeResourceToString(questionnaire)
    val questionnaireResponse = QuestionnaireResponse().apply { id = "a-questionnaire-response" }
    val serializedQuestionniareResponse = printer.encodeResourceToString(questionnaireResponse)
    state.set(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE, serializedQuestionniare)
    state.set(
      QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE_RESPONSE,
      serializedQuestionniareResponse
    )

    val errorMessage =
      assertFailsWith<IllegalArgumentException> { QuestionnaireViewModel(state) }.localizedMessage

    assertThat(errorMessage)
      .isEqualTo("No matching questionnaire response item for questionnaire item a-link-id")
  }

  @Test
  fun stateHasQuestionnaireResponse_moreItemsInQuestionnaireResponse_shouldThrowError() {
    val questionnaire =
      Questionnaire().apply {
        id = "a-questionnaire"
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "a-link-id"
            text = "Basic question"
            type = Questionnaire.QuestionnaireItemType.BOOLEAN
          }
        )
      }
    val serializedQuestionniare = printer.encodeResourceToString(questionnaire)
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "a-questionnaire-response"
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "a-link-id"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = BooleanType(true)
              }
            )
          }
        )
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "a-different-link-id"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = BooleanType(true)
              }
            )
          }
        )
      }
    val serializedQuestionniareResponse = printer.encodeResourceToString(questionnaireResponse)
    state.set(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE, serializedQuestionniare)
    state.set(
      QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE_RESPONSE,
      serializedQuestionniareResponse
    )

    val errorMessage =
      assertFailsWith<IllegalArgumentException> { QuestionnaireViewModel(state) }.localizedMessage

    assertThat(errorMessage)
      .isEqualTo(
        "No matching questionnaire item for questionnaire response item a-different-link-id"
      )
  }

  @Test
  fun questionnaireItemViewItemList_shouldGenerateQuestionnaireItemViewItemList() = runBlocking {
    val questionnaire =
      Questionnaire().apply {
        id = "a-questionnaire"
        addItem(
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "a-link-id"
            text = "Basic questions"
            type = Questionnaire.QuestionnaireItemType.GROUP
            addItem(
              Questionnaire.QuestionnaireItemComponent().apply {
                linkId = "another-link-id"
                text = "Name?"
                type = Questionnaire.QuestionnaireItemType.STRING
              }
            )
          }
        )
      }
    val serializedQuestionnaire = printer.encodeResourceToString(questionnaire)
    state.set(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE, serializedQuestionnaire)
    val viewModel = QuestionnaireViewModel(state)
    var questionnaireItemViewItemList = viewModel.questionnaireItemViewItemList
    questionnaireItemViewItemList[0].questionnaireResponseItemChangedCallback()
    assertThat(questionnaireItemViewItemList.size).isEqualTo(2)
    val firstQuestionnaireItemViewItem = questionnaireItemViewItemList[0]
    val firstQuestionnaireItem = firstQuestionnaireItemViewItem.questionnaireItem
    assertThat(firstQuestionnaireItem.linkId).isEqualTo("a-link-id")
    assertThat(firstQuestionnaireItem.text).isEqualTo("Basic questions")
    assertThat(firstQuestionnaireItem.type).isEqualTo(Questionnaire.QuestionnaireItemType.GROUP)
    assertThat(firstQuestionnaireItemViewItem.questionnaireResponseItem.linkId)
      .isEqualTo("a-link-id")
    val secondQuestionnaireItemViewItem = questionnaireItemViewItemList[1]
    val secondQuestionnaireItem = secondQuestionnaireItemViewItem.questionnaireItem
    assertThat(secondQuestionnaireItem.linkId).isEqualTo("another-link-id")
    assertThat(secondQuestionnaireItem.text).isEqualTo("Name?")
    assertThat(secondQuestionnaireItem.type).isEqualTo(Questionnaire.QuestionnaireItemType.STRING)
    assertThat(secondQuestionnaireItemViewItem.questionnaireResponseItem.linkId)
      .isEqualTo("another-link-id")
  }

  private companion object {
    val printer: IParser = FhirContext.forR4().newJsonParser()
    fun assertResourceEquals(r1: IBaseResource, r2: IBaseResource) {
      assertThat(printer.encodeResourceToString(r1)).isEqualTo(printer.encodeResourceToString(r2))
    }
  }
}
