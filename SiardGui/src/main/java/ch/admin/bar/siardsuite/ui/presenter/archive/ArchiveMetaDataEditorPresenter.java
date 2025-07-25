package ch.admin.bar.siardsuite.ui.presenter.archive;

import ch.admin.bar.siard2.api.Archive;
import ch.admin.bar.siardsuite.framework.ServicesFacade;
import ch.admin.bar.siardsuite.framework.dialogs.Dialogs;
import ch.admin.bar.siardsuite.framework.errors.ErrorHandler;
import ch.admin.bar.siardsuite.framework.i18n.DisplayableText;
import ch.admin.bar.siardsuite.framework.i18n.keys.I18nKey;
import ch.admin.bar.siardsuite.framework.steps.StepperNavigator;
import ch.admin.bar.siardsuite.framework.view.FXMLLoadHelper;
import ch.admin.bar.siardsuite.framework.view.LoadedView;
import ch.admin.bar.siardsuite.model.Tuple;
import ch.admin.bar.siardsuite.model.UserDefinedMetadata;
import ch.admin.bar.siardsuite.service.ArchiveHandler;
import ch.admin.bar.siardsuite.service.database.model.DbmsConnectionData;
import ch.admin.bar.siardsuite.ui.View;
import ch.admin.bar.siardsuite.ui.common.ValidationProperties;
import ch.admin.bar.siardsuite.ui.common.ValidationProperty;
import ch.admin.bar.siardsuite.ui.component.ButtonBox;
import ch.admin.bar.siardsuite.ui.component.SiardTooltip;
import ch.admin.bar.siardsuite.util.I18n;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static ch.admin.bar.siardsuite.ui.component.ButtonBox.Type.DEFAULT;

@Slf4j
public class ArchiveMetaDataEditorPresenter {

    private static final I18nKey TOOLTIP = I18nKey.of("archiveMetadata.view.tooltip");
    private static final I18nKey TITLE = I18nKey.of("archiveMetadata.view.title");
    private static final I18nKey DESCRIPTION = I18nKey.of("archiveMetadata.view.description");
    private static final I18nKey WHAT = I18nKey.of("archiveMetadata.view.titleWhat");
    private static final I18nKey WHO = I18nKey.of("archiveMetadata.view.titleWho");
    private static final I18nKey DATABASE_NAME = I18nKey.of("archiveMetadata.view.databaseName");
    private static final I18nKey DATABASE_DESCRIPTION = I18nKey.of("archiveMetadata.view.databaseDescription");
    private static final I18nKey DELIVERING_OFFICE = I18nKey.of("archiveMetadata.view.deliveringOffice");
    private static final I18nKey DATABASE_CREATION_DATE = I18nKey.of("archiveMetadata.view.databaseCreationDate");
    private static final I18nKey ARCHIVER_NAME = I18nKey.of("archiveMetadata.view.archiverName");
    private static final I18nKey ARCHIVER_CONTACT = I18nKey.of("archiveMetadata.view.archiverContact");
    private static final I18nKey EXPORT_LOCATION_LOB = I18nKey.of("archiveMetadata.view.exportLocationLob");
    private static final I18nKey EXPORT_VIEWS_AS_TABLES = I18nKey.of("archiveMetadata.view.exportViewsAsTables");
    private static final I18nKey SAVE_ARCHIVE = I18nKey.of("button.save.archive");

    @FXML
    protected BorderPane borderPane;

    @FXML
    Text titleText = new Text();
    @FXML
    Text descriptionText = new Text();
    @FXML
    Text titleWhat = new Text();
    @FXML
    Text titleWho = new Text();
    @FXML
    TextField name;
    @FXML
    public Label descriptionLabel;
    @FXML
    TextArea description;
    @FXML
    TextField owner;
    //@FXML
    //TextField dataOriginTimespan;
    @FXML
    TextField archiverName;
    @FXML
    public Label archiverContactLabel;
    @FXML
    TextArea archiverContact;
    @FXML
    protected ButtonBox buttonsBox;
    @FXML
    public MFXButton infoButton;
    @FXML
    public Label nameLabel;
    @FXML
    public Label ownerLabel;
    @FXML
    public Label dataOriginTimespanLabel;
    @FXML
    private ComboBox<String> dataOriginTimespan;
    @FXML
    public Label archiverLabel;
    @FXML
    public Label ownerValidationMsg;
    @FXML
    public Label dataOriginTimespanValidationMsg;
    @FXML
    public Label lobExportLabel;
    @FXML
    public TextField lobExportLocation;
    @FXML
    public CheckBox exportViewsAsTables;

    private ErrorHandler errorHandler;
    private Dialogs dialogs;
    private ArchiveHandler archiveHandler;

    private Archive archive;

    public void init(
            final Archive archive,
            final DbmsConnectionData connectionData,
            final StepperNavigator<Tuple<UserDefinedMetadata, DbmsConnectionData>> navigator,
            final ErrorHandler errorHandler,
            final Dialogs dialogs,
            final ArchiveHandler archiveHandler
    ) {
        this.archiveHandler = archiveHandler;
        this.errorHandler = errorHandler;
        this.dialogs = dialogs;

        this.archive = archive;

        this.bindTexts();

        this.buttonsBox = new ButtonBox().make(DEFAULT);
        this.borderPane.setBottom(buttonsBox);
        this.buttonsBox.previous().setOnAction(event -> navigator.previous());
        this.buttonsBox.cancel().setOnAction((event) -> dialogs
                .open(View.ARCHIVE_ABORT_DIALOG));
        this.buttonsBox.next().setOnAction((event) -> {
            tryReadValidUserDefinedMetadata()
                    .ifPresent(userDefinedMetadata ->
                            navigator.next(new Tuple<>(userDefinedMetadata, connectionData)));
        });

        val saveArchiveButton = new MFXButton();
        saveArchiveButton.textProperty().bind(DisplayableText.of(SAVE_ARCHIVE).bindable());
        saveArchiveButton.setOnAction(event -> this.saveOnlyMetaData());

        this.buttonsBox.append(saveArchiveButton);
        initFields(archive);

        new SiardTooltip(TOOLTIP).showOnMouseOn(infoButton);
    }

    private void bindTexts() {
        this.titleText.textProperty().bind(DisplayableText.of(TITLE).bindable());
        this.descriptionText.textProperty().bind(DisplayableText.of(DESCRIPTION).bindable());
        this.titleWhat.textProperty().bind(DisplayableText.of(WHAT).bindable());
        this.titleWho.textProperty().bind(DisplayableText.of(WHO).bindable());
        this.nameLabel.textProperty().bind(DisplayableText.of(DATABASE_NAME).bindable());
        this.descriptionLabel.textProperty().bind(DisplayableText.of(DATABASE_DESCRIPTION).bindable());
        this.ownerLabel.textProperty().bind(DisplayableText.of(DELIVERING_OFFICE).bindable());
        this.dataOriginTimespanLabel.textProperty().bind(DisplayableText.of(DATABASE_CREATION_DATE).bindable());
        this.archiverLabel.textProperty().bind(DisplayableText.of(ARCHIVER_NAME).bindable());
        this.archiverContactLabel.textProperty().bind(DisplayableText.of(ARCHIVER_CONTACT).bindable());
        this.lobExportLabel.textProperty().bind(DisplayableText.of(EXPORT_LOCATION_LOB).bindable());
        this.exportViewsAsTables.textProperty().bind(DisplayableText.of(EXPORT_VIEWS_AS_TABLES).bindable());
    }

    private File showFileChooserToSelectTargetArchive(String databaseName) { // TODO Move to dialogs
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("export.choose-location.text"));
        fileChooser.setInitialFileName(databaseName + ".siard");
        final FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("SIARD files", "*.siard");
        fileChooser.getExtensionFilters().add(extensionFilter);
        return fileChooser.showSaveDialog(titleText.getScene().getWindow());
    }

    private void saveOnlyMetaData() {
        tryReadValidUserDefinedMetadata()
                .ifPresent(userDefinedMetadata -> {
                    val archiveCopy = archiveHandler.copy(archive, userDefinedMetadata.getSaveAt());
                    archiveHandler
                            .write(archiveCopy, userDefinedMetadata)
                            .save(archiveCopy, userDefinedMetadata.getSaveAt());
                });
    }

    private Optional<UserDefinedMetadata> tryReadValidUserDefinedMetadata() {
        if (this.validateProperties()) {
            File targetArchive = this.showFileChooserToSelectTargetArchive(this.name.getText());

            val lobFolder = Optional.ofNullable(lobExportLocation.getText())
                    .filter(text -> !text.isEmpty())
                    .map(text -> text.replace("\\", "/")) // replace backslashes with slashes that work on all platforms
                    .map(text -> {
                        try {
                            return new URI(text);
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    });

            if (targetArchive != null) {
                // 선택한 시간대에 따른 현재 시간 가져오기
                String timestamp = getCurrentTimestampForTimeZone(this.dataOriginTimespan.getValue());

                return Optional.of(
                        UserDefinedMetadata.builder()
                                .dbName(this.name.getText())
                                .description(Optional.ofNullable(this.description.getText()).filter(text -> !text.isEmpty()))
                                .owner(this.owner.getText())
                                .dataOriginTimespan(timestamp) // 타임스탬프 저장
                                .archiverName(Optional.ofNullable(this.archiverName.getText()).filter(text -> !text.isEmpty()))
                                .archiverContact(Optional.ofNullable(this.archiverContact.getText()).filter(text -> !text.isEmpty()))
                                .lobFolder(lobFolder)
                                .saveAt(targetArchive)
                                .exportViewsAsTables(exportViewsAsTables.isSelected())
                                .selectedSchemaTableMap(archive.getSelectedSchemaTableMap())
                                .formDataSet(archive.getFormDataSet())
                                .build()
                );
            }
        }

        return Optional.empty();
    }

    private void initFields(final Archive archive) {
        val metadata = archive.getMetaData();

        this.name.setText(metadata.getDbName());
        this.description.setText(metadata.getDescription());
        this.owner.setText(removePlaceholder(metadata.getDataOwner()));
        //this.dataOriginTimespan.setValue(removePlaceholder(metadata.getDataOriginTimespan()));
        this.dataOriginTimespan.setValue("KTC");
        handleTimeZoneSelection();

        this.archiverName.setText(metadata.getArchiver());
        this.archiverContact.setText(metadata.getArchiverContact());
        Optional.ofNullable(metadata.getLobFolder())
                .ifPresent(lobFolder -> this.lobExportLocation.setText(lobFolder.toString()));

        // ComboBox 선택 이벤트 핸들러 추가
        this.dataOriginTimespan.setOnAction(event -> handleTimeZoneSelection());
    }

    // 시간대 선택 핸들러
    private void handleTimeZoneSelection() {
        String selectedTimeZone = dataOriginTimespan.getValue();
        if (selectedTimeZone != null) {
            String timestamp = getCurrentTimestampForTimeZone(selectedTimeZone);

            // 바인딩 해제
            if (dataOriginTimespanLabel.textProperty().isBound()) {
                dataOriginTimespanLabel.textProperty().unbind();
            }

            // 텍스트 업데이트
            dataOriginTimespanLabel.setText("Current Time: " + timestamp);
        }
    }

    // 시간대에 따른 현재 시간 반환
    private String getCurrentTimestampForTimeZone(String timeZone) {
        ZoneId zoneId = "KTC".equals(timeZone) ? ZoneId.of("Asia/Seoul") : ZoneId.of("UTC");
        return ZonedDateTime.now(zoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String removePlaceholder(String value) {
        return (value.equals("(...)") ? "" : value);
    }

    private boolean validateProperties() {
        List<ValidationProperty> validationProperties = Arrays.asList(
                new ValidationProperty(owner,
                        ownerValidationMsg,
                        "archiveMetaData.owner.missing"), // owner 필드 유효성 검사 유지
                new ValidationProperty(dataOriginTimespan,
                        dataOriginTimespanValidationMsg,
                        "archiveMetaData.timespan.missing") // dataOriginTimespan 필드 유효성 검사 추가
        );

        return validationProperties.stream().allMatch(ValidationProperty::validate);
    }

    public static LoadedView<ArchiveMetaDataEditorPresenter> load(
            final Tuple<Archive, DbmsConnectionData> data,
            final StepperNavigator<Tuple<UserDefinedMetadata, DbmsConnectionData>> navigator,
            final ServicesFacade servicesFacade
    ) {
        val loaded = FXMLLoadHelper.<ArchiveMetaDataEditorPresenter>load("fxml/archive/archive-metadata-editor.fxml");
        loaded.getController().init(
                data.getValue1(),
                data.getValue2(),
                navigator,
                servicesFacade.errorHandler(),
                servicesFacade.dialogs(),
                servicesFacade.getService(ArchiveHandler.class)
        );

        return loaded;
    }
}
