package org.molgenis.questionnaires.controller;

import static java.util.Objects.requireNonNull;
import static org.molgenis.questionnaires.meta.QuestionnaireStatus.NOT_STARTED;

import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.questionnaires.meta.Questionnaire;
import org.molgenis.questionnaires.meta.QuestionnaireStatus;
import org.molgenis.questionnaires.response.QuestionnaireResponse;
import org.molgenis.questionnaires.service.QuestionnaireService;
import org.molgenis.security.user.UserAccountService;
import org.molgenis.web.PluginController;
import org.molgenis.web.menu.MenuReaderService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(QuestionnaireController.URI)
public class QuestionnaireController extends PluginController {
  public static final String ID = "questionnaires";
  public static final String URI = PluginController.PLUGIN_URI_PREFIX + ID;
  private static final String KEY_BASE_URL = "baseUrl";

  private static final String QUESTIONNAIRE_VIEW = "view-questionnaire";

  private final QuestionnaireService questionnaireService;
  private final MenuReaderService menuReaderService;
  private final UserAccountService userAccountService;

  public QuestionnaireController(
      QuestionnaireService questionnaireService,
      MenuReaderService menuReaderService,
      UserAccountService userAccountService) {
    super(URI);
    this.questionnaireService = requireNonNull(questionnaireService);
    this.menuReaderService = requireNonNull(menuReaderService);
    this.userAccountService = requireNonNull(userAccountService);
  }

  /** Loads the questionnaire view */
  @GetMapping("/**")
  public String initView(Model model) {
    model.addAttribute(KEY_BASE_URL, menuReaderService.findMenuItemPath(ID));
    model.addAttribute("username", userAccountService.getCurrentUser().getUsername());
    return QUESTIONNAIRE_VIEW;
  }

  /**
   *
   *
   * <h1>Internal Questionnaire API</h1>
   *
   * Retrieves a list of all the available questionnaires
   *
   * @return A list of {@link QuestionnaireResponse}
   */
  @ResponseBody
  @GetMapping(value = "/list")
  public List<QuestionnaireResponse> getQuestionnaires() {
    return questionnaireService
        .getQuestionnaires()
        .map(this::createQuestionnaireResponse)
        .collect(Collectors.toList());
  }

  /**
   *
   *
   * <h1>Internal Questionnaire API</h1>
   *
   * Starts a questionnaire
   *
   * @param id A questionnaire ID
   */
  @GetMapping(value = "/start/{id}")
  @ResponseBody
  public QuestionnaireResponse startQuestionnaire(@PathVariable("id") String id) {
    return questionnaireService.startQuestionnaire(id);
  }

  /**
   *
   *
   * <h1>Internal Questionnaire API</h1>
   *
   * Retrieves a submission text for a questionnaire
   *
   * @param id A questionnaire ID
   * @return A "thank you" text shown on submit of a questionnaire
   */
  @ResponseBody
  @GetMapping("/submission-text/{id}")
  @SuppressWarnings("javasecurity:S5131") // id is validated in questionnaireService
  public String getQuestionnaireSubmissionText(@PathVariable("id") String id) {
    return questionnaireService.getQuestionnaireSubmissionText(id);
  }

  /**
   * Create a {@link QuestionnaireResponse} based on an {@link EntityType} Will set status to {@link
   * QuestionnaireStatus}.OPEN if there is a data entry for the current user.
   *
   * @param entityType A Questionnaire EntityType
   * @return A {@link QuestionnaireResponse}
   */
  private QuestionnaireResponse createQuestionnaireResponse(EntityType entityType) {
    String entityTypeId = entityType.getId();

    QuestionnaireStatus status = NOT_STARTED;
    Questionnaire questionnaireEntity = questionnaireService.findQuestionnaireEntity(entityTypeId);
    if (questionnaireEntity != null) {
      status = questionnaireEntity.getStatus();
    }

    String lng = LocaleContextHolder.getLocale().getLanguage();

    return QuestionnaireResponse.create(
        entityTypeId, entityType.getLabel(lng), entityType.getDescription(lng), status);
  }
}
