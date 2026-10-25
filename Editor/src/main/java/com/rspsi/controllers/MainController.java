package com.rspsi.controllers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import com.jfoenix.controls.JFXButton;
import com.rspsi.util.Settings;
import javafx.scene.control.*;
import org.major.map.RenderFlags;

import com.google.common.primitives.Doubles;
import com.jagex.Client;
import com.jagex.map.SceneGraph;
import com.jagex.util.BitFlag;
import com.jfoenix.controls.JFXCheckBox;
import com.rspsi.ui.MainWindow;
import com.rspsi.ui.Testing;
import com.rspsi.controls.SwatchControl;
import com.rspsi.controls.WindowControls;
import com.rspsi.core.misc.BrushType;
import com.rspsi.core.misc.ToolType;
import com.rspsi.options.Options;
import com.rspsi.swatches.SwatchType;
import com.rspsi.tools.BridgeBuilder;
import com.rspsi.util.AlwaysSelectToggleGroup;
import com.rspsi.util.ChangeListenerUtil;
import com.rspsi.util.FXDialogs;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.util.converter.IntegerStringConverter;
import lombok.Getter;

@Getter
public class MainController {
	private static final String PROPERTY_CARD_ORDER = "propertyCardOrder";
	private static final String PROPERTY_CARD_WIDTH_PREFIX = "propertyCardWidth.";
	private final Map<String, VBox> propertyCards = new LinkedHashMap<>();
	private VBox resizingCard;
	private VBox draggingCard;
	private double resizeStartX;
	private double resizeStartWidth;
	private boolean customPropertyLayout;

	@FXML
	private SplitPane editorWorkspace;

	@FXML
	private FlowPane propertyBar;

	@FXML
	private VBox toolsPanel, brushPropertyCard, heightPropertyCard, objectPropertyCard, planePropertyCard;

	@FXML
	private Label brushShapeLabel;

	@FXML
	private VBox mapEmptyState;

	@FXML
	private Button emptyNewMapButton, emptyCoordinatesButton, emptyFilesButton, emptyPackButton;

	@FXML
	private MenuItem changeViewDist;

	@FXML
	private MenuItem openAsPackBtn;

	@FXML
	private MenuItem saveToCacheBtn;

	@FXML
	private Menu packToCacheMenu;

	@FXML
	private MenuItem uploadCacheToMainBtn;

	@FXML
	private MenuItem showMapIndexEditor;

	@FXML
	private HBox grabBar;

	@FXML
	private HBox controlBox;

	@FXML
	private HBox windowControlsContainer;

	@FXML
	private Label titleLabel;

	@FXML
	private Label statusLabel;

	@FXML
	private MenuBar toolbar;

	@FXML
	private MenuItem newMapButton;

	@FXML
	private Menu openRecentMenu;

	@FXML
	private MenuItem openCoordinateButton;

	@FXML
	private MenuItem openHashButton;

	@FXML
	private MenuItem openFileButton;

	@FXML
	private MenuItem saveAsMenuItem;

	@FXML
	private MenuItem preferencesMenuItem;

	@FXML
	private MenuItem quitMenuItem;

	@FXML
	private SeparatorMenuItem quitSeparator;

	@FXML
	private MenuItem copySelectedTilesBtn;

	@FXML
	private MenuItem deleteSelectedTilesBtn;

	@FXML
	private MenuItem pasteTilesBtn;

	@FXML
	private MenuItem addObjectToSwatchBtn;

	@FXML
	private CheckMenuItem hiddenTilesCheckItem;

	@FXML
	private CheckMenuItem allHeightsCheckItem;

	@FXML
	private CheckMenuItem disableBlendingCheckItem;

	@FXML
	private CheckMenuItem showObjectsCheckItem;

	@FXML
	private CheckMenuItem rememberSize;

	@FXML
	private CheckMenuItem rememberLocation;

	@FXML
	private CheckMenuItem saveByGroupName;

	@FXML
	private CheckMenuItem showOverlaysCheckItem;

	@FXML
	private MenuItem forceMapUpdateBtn;

	@FXML
	private MenuItem importTilesBtn;

	@FXML
	private MenuItem exportTilesBtn;

	@FXML
	private MenuItem showObjectViewBtn;

	@FXML
	private CheckMenuItem showFPSCheckItem;
	@FXML
	private CheckMenuItem showTileDataCheckItem;

	@FXML
	private MenuItem openTutorialBtn;

	@FXML
	private MenuItem contactMeBtn;

	@FXML
	private HBox dockContainer;

	@FXML
	private AnchorPane leftBar;

	@FXML
	private ToggleButton selectTileBtn;

	@FXML
	private ToggleGroup toolGroup;

	@FXML
	private ToggleButton selectObjectBtn;

	@FXML
	private ToggleButton paintTileBtn;

	@FXML
	private ToggleButton heightModifyBtn;

	@FXML
	private ToggleButton deleteObjectBtn;

	@FXML
	private ToggleButton paintFillBtn;

	@FXML
	private ToggleButton heightFillBtn;

	@FXML
	private ToggleButton moveObjectBtn;

	@FXML
	private JFXButton returnToLauncher;


	@FXML
	private AnchorPane gamePane;

	@FXML
	private AnchorPane mapPane;

	@FXML
	private Slider brushSizeSlider;

	@FXML
	private Spinner<Integer> brushSizeSpinner;

	@FXML
	private ComboBox<BrushType> brushTypeSelection;

	@FXML
	private Slider heightLevelSlider;

	@FXML
	private ComboBox<String> objectSelectionType;

	@FXML
	private Spinner<Integer> currentHeightSpinner;

	@FXML
	private AnchorPane rightBar;

	@FXML
	private Tooltip brushSizeTooltip;

	@FXML
	private Tooltip heightLevelTooltip;

	@FXML
	private Button decreaseBrushSizeBtn;

	@FXML
	private Button increaseBrushSizeBtn;

	@FXML
	private MenuItem setTileHeightMenuItem1;

	@FXML
	private TabPane toolsTabPane;

	@FXML
	private MenuItem undoMenuItem;

	@FXML
	private MenuItem redoMenuItem;

	@FXML
	private CheckMenuItem showBlockedFlag;

	@FXML
	private CheckMenuItem showBridgeFlag;

	@FXML
	private CheckMenuItem showLowestFlag;

	@FXML
	private CheckMenuItem showDisableFlag;

	@FXML
	private JFXCheckBox unwalkableCheck;

	@FXML
	private JFXCheckBox bridgeCheck;

	@FXML
	private JFXCheckBox forceLowestCheck;
	
	@FXML
	private JFXCheckBox disableRenderCheck;

	@FXML
	private JFXCheckBox drawOnLowerZCheck;
	

	@FXML
	private JFXCheckBox absoluteHeightCheck;

	@FXML
	private ToggleButton setFlagBtn;
	
    @FXML
    private ToggleButton paintOverlayBtn;

    @FXML
    private ToggleButton paintUnderlayBtn;

	@FXML
	private MenuItem copyTileFlags;


	@FXML
	private CheckMenuItem showLowerZFlag;


	@FXML
	private MenuItem copyTileHeights;

	@FXML
	private TextField tileHeightTextBox;

	@FXML
	private Button tileHeightTextButton;

	@FXML
	private MenuItem setFlagsToTiles;

	@FXML
	private MenuItem setHeightsToTiles;

	@FXML
	private MenuItem showFullMap;

	@FXML
	private MenuItem reloadSwatchesBtn;
	@FXML
	private MenuItem reloadModelsBtn;

	@FXML
	private MenuItem fixHeightsBtn;

	@FXML
	private MenuItem getOverlayFromTile;

	@FXML
	private MenuItem getUnderlayFromTile;

	@FXML
	private CheckMenuItem displayOverlayIds;

	@FXML
	private CheckMenuItem displayUnderlayIds;

	@FXML
	private CheckMenuItem displayTileHeights;

	@FXML
	private CheckMenuItem showMapIconObjs;

	@FXML
	private CheckMenuItem showAnimsMenuItem;

	@FXML
	private TabPane mainTabPane;

	@FXML
	private MenuItem setOverlays;

	@FXML
	private MenuItem setUnderlays;

	@FXML
	private CheckMenuItem simulateBridges;

	@FXML
	private MenuItem showRemapperBtn;

	@FXML
	private MenuItem convertLandscapeBtn;

	@FXML
	private MenuItem setRelativeHeight;
	
	@FXML
	private MenuItem generateBridgeBtn;
	
	@FXML
	private VBox root;
	

    @FXML
    private Menu fileMenu, editMenu, viewMenu, tilesMenu, toolsMenu, helpMenu, debugMenu;

	@FXML
	private Menu settingsMenu;

	public void initializeToolButtons() {
		this.toolGroup.selectedToggleProperty().addListener((observable, oldVal, newVal) -> {
			if (newVal == null) {
				if (toolGroup.getProperties().get("deselect") == null) {
					oldVal.setSelected(true);
				} else {
					toolGroup.getProperties().remove("deselect");
				}
			}
		});
		ChangeListenerUtil.addListener(() -> SceneGraph.mouseWasDown = true, Options.currentTool);
		ChangeListenerUtil.addListener(true, () -> {
			Options.currentTool.set(ToolType.SELECT_TILE);
		}, selectTileBtn.selectedProperty());

		ChangeListenerUtil.addListener(true, () -> {
			Options.currentTool.set(ToolType.MODIFY_HEIGHT);
		}, heightModifyBtn.selectedProperty());

		ChangeListenerUtil.addListener(true, () -> {
			Options.currentTool.set(ToolType.SELECT_OBJECT);
		}, selectObjectBtn.selectedProperty());
		ChangeListenerUtil.addListener(true, () -> {
			Options.currentTool.set(ToolType.DELETE_OBJECT);
		}, deleteObjectBtn.selectedProperty());
		
		ChangeListenerUtil.addListener(true, () -> {
			Options.currentTool.set(ToolType.PAINT_OVERLAY);
		}, paintOverlayBtn.selectedProperty());
		ChangeListenerUtil.addListener(true, () -> {
			Options.currentTool.set(ToolType.PAINT_UNDERLAY);
		}, paintUnderlayBtn.selectedProperty());
		
		ChangeListenerUtil.addListener(true, () -> {
			Options.currentTool.set(ToolType.SET_FLAGS);
		}, setFlagBtn.selectedProperty());
		

	}

	public void loadTabs(MainWindow application) {

		for (int i = 0; i < 3; i++) {
			SwatchType swatchType = SwatchType.getById(i);
			SwatchControl swatch = new SwatchControl(swatchType);
			switch (swatchType) {
			case OBJECT:
				application.setObjectSwatch(swatch);
				break;
			case OVERLAY:

				FlowPane flowPane = new FlowPane();
				flowPane.setPadding(new Insets(4, 4, 4, 4));
				flowPane.setOrientation(Orientation.HORIZONTAL);
				flowPane.setRowValignment(VPos.CENTER);
				flowPane.setColumnHalignment(HPos.CENTER);
				flowPane.setHgap(6);
				flowPane.setVgap(6);
				ToggleGroup tg = new ToggleGroup();

				AlwaysSelectToggleGroup.setup(tg);
				ToggleButton[] shapeButtons = new ToggleButton[13];
				for (int type = 0; type < 13; type++) {
					Pane g = Testing.generateImage(type);
					ToggleButton btn = new ToggleButton();
					shapeButtons[type] = btn;
					tg.getProperties().put(type, btn);
					btn.setAlignment(Pos.CENTER);
					btn.setMaxSize(36, 36);
					btn.setPrefSize(36, 36);
					btn.setMinSize(36, 36);
					btn.setToggleGroup(tg);
					btn.setGraphic(g);
					btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					final int shapeType = type;
					btn.setOnAction(evt -> {

						Options.overlayPaintShapeId.set(shapeType);
					});
					flowPane.getChildren().add(btn);
					if (type == 1) {
						btn.setSelected(true);
					}

				}
				flowPane.setAlignment(Pos.CENTER);
				swatch.getController().getVboxContainer().setSpacing(10);
				swatch.setOverlayShapeGroup(tg);
				swatch.getController().getVboxContainer().getChildren().add(0, flowPane);
				application.setOverlaySwatch(swatch);

				//TODO Find a less aids way of doing this

				break;
			case UNDERLAY:
				application.setUnderlaySwatch(swatch);
				break;

			}

			Tab tab = new Tab(swatchType.toString());
			tab.setContent(swatch);
			toolsTabPane.getTabs().add(i, tab);
		}

		toolsTabPane.getSelectionModel().select(SwatchType.OBJECT.getId());

	}

	public void onLoad(MainWindow application) {
		initializeToolButtons();
		loadTabs(application);
		initializeResponsiveLayout();
		emptyNewMapButton.setOnAction(event -> newMapButton.fire());
		emptyCoordinatesButton.setOnAction(event -> openCoordinateButton.fire());
		emptyFilesButton.setOnAction(event -> openFileButton.fire());
		emptyPackButton.setOnAction(event -> openAsPackBtn.fire());

		ChangeListenerUtil.addListener(() -> this.tileHeightTextBox.setText(this.heightLevelSlider.getValue() + ""),
				Options.tileHeightLevel);

		currentHeightSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3, 0));
		brushSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16,
				(int) brushSizeSlider.getValue() + 1));
		if (Boolean.getBoolean("rspsi.embedded")) {
			controlBox.setVisible(false);
			controlBox.setManaged(false);
			windowControlsContainer.setVisible(false);
			windowControlsContainer.setManaged(false);
			quitMenuItem.setVisible(false);
			quitSeparator.setVisible(false);
			rememberSize.setVisible(false);
			rememberLocation.setVisible(false);
			settingsMenu.setVisible(false);
			helpMenu.setVisible(false);
		} else {
			windowControls = WindowControls.addWindowControls(application.getStage(), grabBar, controlBox);
		}

		// windowControls.getResizeHelper().setMinWidth(1240);
		objectSelectionType.getItems().add("ALL");
		objectSelectionType.getSelectionModel().select(0);
		IntStream.range(0, 23).forEach(act -> objectSelectionType.getItems().add(String.valueOf(act)));

		Options.objectSelectionType.bind(objectSelectionType.getSelectionModel().selectedIndexProperty());

		brushTypeSelection.getItems().addAll(BrushType.values());
		Options.brushType.bindBidirectional(brushTypeSelection.valueProperty());
		// brushTypeSelection.get
		// brushTypeSelection.
		brushTypeSelection.getSelectionModel().select(0);

		ChangeListenerUtil.addListener(() -> SceneGraph.minimapUpdate = true, mapPane.widthProperty());

		Options.brushSize.bindBidirectional(brushSizeSlider.valueProperty());
		brushSizeSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && brushSizeSlider.getValue() != newValue - 1) {
				brushSizeSlider.setValue(newValue - 1);
			}
		});
		brushSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
			int displayedSize = newValue.intValue() + 1;
			if (!Integer.valueOf(displayedSize).equals(brushSizeSpinner.getValue())) {
				brushSizeSpinner.getValueFactory().setValue(displayedSize);
			}
		});
		brushSizeTooltip.textProperty().set(Options.brushSize.get() + 1 + "");
		ChangeListenerUtil.addListener(() -> brushSizeTooltip.textProperty().set(Options.brushSize.get() + 1 + ""),
				Options.brushSize);

		Options.showDebug.bindBidirectional(showFPSCheckItem.selectedProperty());
		Options.showTileInformation.bindBidirectional(showTileDataCheckItem.selectedProperty());

		Options.tileHeightLevel.bindBidirectional(heightLevelSlider.valueProperty());
		// ChangeListenerUtil.addListener(() ->
		// heightLevelTooltip.setText(Options.tileHeightLevel.get() + ""),
		// Options.tileHeightLevel);

		Options.absoluteHeightProperty.bindBidirectional(this.absoluteHeightCheck.selectedProperty());
		Options.disableBlending.bindBidirectional(this.disableBlendingCheckItem.selectedProperty());
		Options.showObjects.bindBidirectional(this.showObjectsCheckItem.selectedProperty());
		Options.allHeightsVisible.bindBidirectional(this.allHeightsCheckItem.selectedProperty());
		Options.showOverlay.bindBidirectional(this.showOverlaysCheckItem.selectedProperty());

		Options.showOverlayNumbers.bindBidirectional(this.displayOverlayIds.selectedProperty());
		Options.showUnderlayNumbers.bindBidirectional(this.displayUnderlayIds.selectedProperty());
		Options.showTileHeightNumbers.bindBidirectional(this.displayTileHeights.selectedProperty());

		Options.showBlockedFlag.bindBidirectional(this.showBlockedFlag.selectedProperty());
		Options.showBridgeFlag.bindBidirectional(this.showBridgeFlag.selectedProperty());

		Options.showForceLowestPlaneFlag.bindBidirectional(this.showLowestFlag.selectedProperty());
		Options.showDisableRenderFlag.bindBidirectional(this.showDisableFlag.selectedProperty());
		Options.showLowerZFlag.bindBidirectional(this.showLowerZFlag.selectedProperty());

		Options.showMinimapFunctionModels.bindBidirectional(this.showMapIconObjs.selectedProperty());
		Options.loadAnimations.bindBidirectional(this.showAnimsMenuItem.selectedProperty());

		boolean remeberSize = (Boolean) Settings.properties.getOrDefault("remember_size",true);
		boolean remeberLocation = (Boolean) Settings.properties.getOrDefault("remember_location",false);
		boolean saveGroupName = (Boolean) Settings.properties.getOrDefault("save_group_name",false);

		ChangeListenerUtil.addListener(() -> {
			if(remeberSize) {
				Settings.properties.put("remember_size", false);
			} else {
				Settings.properties.put("remember_size", true);
			}
			rememberSize.setSelected(remeberSize);
			Settings.saveSettings();
		}, Options.rememberEditorSize);

		ChangeListenerUtil.addListener(() -> {
			if(remeberLocation) {
				Settings.properties.put("remember_location", false);
			} else {
				Settings.properties.put("remember_location", true);
			}
			rememberLocation.setSelected(remeberLocation);
			Settings.saveSettings();
		}, Options.rememberEditorLocation);


		ChangeListenerUtil.addListener(() -> {
			if(saveGroupName) {
				Settings.properties.put("save_group_name", false);
			} else {
				Settings.properties.put("save_group_name", true);
			}
			saveByGroupName.setSelected(saveGroupName);
			Settings.saveSettings();
		}, Options.saveByGroupName);

		rememberSize.setSelected(remeberSize);
		rememberLocation.setSelected(remeberLocation);
		saveByGroupName.setSelected(saveGroupName);

		Options.rememberEditorSize.bindBidirectional(this.rememberSize.selectedProperty());
		Options.rememberEditorLocation.bindBidirectional(this.rememberLocation.selectedProperty());
		Options.saveByGroupName.bindBidirectional(this.saveByGroupName.selectedProperty());

		decreaseBrushSizeBtn.setOnAction(act -> brushSizeSlider.adjustValue(brushSizeSlider.getValue() - 1));
		increaseBrushSizeBtn.setOnAction(act -> brushSizeSlider.adjustValue(brushSizeSlider.getValue() + 1));
		setTileHeightMenuItem1.setOnAction(act -> {
			String value = FXDialogs.showTextInput(application.getStage(), "Set brush size",
					"Enter a brush size from 1 to 16.", String.valueOf((int) brushSizeSlider.getValue() + 1));
			if (value == null || value.trim().isEmpty()) return;
			try {
				int size = Integer.parseInt(value.trim());
				brushSizeSlider.setValue(Math.max(1, Math.min(16, size)) - 1);
			} catch (NumberFormatException exception) {
				FXDialogs.showError(application.getStage(), "Invalid brush size",
						"Brush size must be a whole number from 1 to 16.");
			}
		});

		Runnable calculateFlags = () -> {
			BitFlag flag = new BitFlag();
			if (this.unwalkableCheck.isSelected())
				flag.flag(RenderFlags.BLOCKED_TILE);
			if (this.bridgeCheck.isSelected())
				flag.flag(RenderFlags.BRIDGE_TILE);
			if (this.forceLowestCheck.isSelected())
				flag.flag(RenderFlags.FORCE_LOWEST_PLANE);
			if (this.drawOnLowerZCheck.isSelected())
				flag.flag(RenderFlags.RENDER_ON_LOWER_Z);
			if (this.disableRenderCheck.isSelected())
				flag.flag(RenderFlags.DISABLE_RENDERING);

			Options.tileFlags.set(flag);
		};
		this.heightLevelSlider.valueProperty()
				.addListener((observable, oldVal, newVal) -> this.tileHeightTextBox.setText(newVal.intValue() + ""));
		this.tileHeightTextButton.setOnAction(
				evt -> { 
					this.heightLevelSlider.adjustValue(Doubles.tryParse(this.tileHeightTextBox.getText()));
					System.out.println(this.heightLevelSlider.valueProperty().get() + " : " + this.heightLevelSlider.valueProperty().intValue());
					this.getTileHeightTextBox().textProperty().set(this.getHeightLevelSlider().valueProperty().intValue() + "");
					this.tileHeightTextButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("notset"), false);
				});
		UnaryOperator<TextFormatter.Change> integerFilter = change -> {
			String newText = change.getControlNewText();
			
			if (newText.matches("-?([0-9]*)?")) {
				return change;
			}
			return null;
		};
		this.tileHeightTextBox.textProperty().addListener((observable, oldVal, newVal) -> {
			int val = Integer.parseInt(newVal);
			if (val != heightLevelSlider.valueProperty().intValue()) {
				this.tileHeightTextButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("notset"), true);
				System.out.println(val + " : " + this.heightLevelSlider.valueProperty().get());
			} else {
				this.tileHeightTextButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("notset"), false);
			}
		});
		tileHeightTextBox.setTextFormatter(new TextFormatter<Integer>(new IntegerStringConverter(),
				heightLevelSlider.valueProperty().intValue(), integerFilter));
		tileHeightTextBox.addEventFilter(KeyEvent.ANY, evt -> {
			if(evt.getCode() == KeyCode.ENTER) {
				tileHeightTextButton.fire();
			}
		});

		ChangeListenerUtil.addListener(calculateFlags, unwalkableCheck.selectedProperty(),
				bridgeCheck.selectedProperty(), forceLowestCheck.selectedProperty(),
				drawOnLowerZCheck.selectedProperty(), disableRenderCheck.selectedProperty());

		Options.showHiddenTiles.bindBidirectional(hiddenTilesCheckItem.selectedProperty());
		Options.currentHeight.bind(currentHeightSpinner.valueProperty());

		Options.simulateBridgesProperty.bindBidirectional(simulateBridges.selectedProperty());
		
		generateBridgeBtn.setOnAction(evt -> {
			try {
				BridgeBuilder.buildBridge();
			} catch (Exception e) {
				FXDialogs.showError(application.getStage().getOwner(),"Error while generating bridge!", "Message: " + e.getMessage());
			}
		});

		Options.currentTool.addListener((observable, oldVal, newVal) -> {

			deselectTools();
			if(Client.getSingleton() != null && Client.getSingleton().sceneGraph != null) {
				Client.getSingleton().sceneGraph.resetTiles();
				Client.getSingleton().sceneGraph.resetLastHighlightedTiles();
			}
			
			/*if (oldVal == ToolType.PAINT_OVERLAY) {
				Options.overlayPaintId.set(-1);
				application.overlaySwatch.deselect();
			} else if (oldVal == ToolType.PAINT_UNDERLAY) {
				Options.underlayPaintId.set(-1);
				application.underlaySwatch.deselect();
			} else if (oldVal == ToolType.SPAWN_OBJECT) {
				Options.currentObject.set(null);
				application.objectSwatch.deselect();
			}*/

			if (newVal == ToolType.SELECT_OBJECT) {
				this.selectObjectBtn.setSelected(true);
			} else if (newVal == ToolType.DELETE_OBJECT) {
				this.deleteObjectBtn.setSelected(true);
			} else if (newVal == ToolType.MODIFY_HEIGHT) {
				this.heightModifyBtn.setSelected(true);
			} else if (newVal == ToolType.SET_FLAGS) {
				this.setFlagBtn.setSelected(true);
			} else if (newVal == ToolType.SELECT_TILE) {
				this.selectTileBtn.setSelected(true);
			} else if(newVal == ToolType.PAINT_OVERLAY) {
				this.paintOverlayBtn.setSelected(true);
			} else if(newVal == ToolType.PAINT_UNDERLAY) {
				this.paintUnderlayBtn.setSelected(true);
			}

		});

		this.setRelativeHeight.setOnAction(evt -> Client.getSingleton().sceneGraph.setAbsoluteHeight());
		this.setFlagsToTiles.setOnAction(
				evt -> SceneGraph.onCycleEnd.add(() -> Client.getSingleton().sceneGraph.setSelectedFlags()));
		this.setHeightsToTiles.setOnAction(
				evt -> SceneGraph.onCycleEnd.add(() -> Client.getSingleton().sceneGraph.setSelectedHeight()));
		this.setOverlays.setOnAction(
				evt -> SceneGraph.onCycleEnd.add(() -> Client.getSingleton().sceneGraph.setSelectedOverlays()));
		this.setUnderlays.setOnAction(
				evt -> SceneGraph.onCycleEnd.add(() -> Client.getSingleton().sceneGraph.setSelectedUnderlays()));


	}

	private void initializeResponsiveLayout() {
		initializeDockablePropertyCards();
		propertyBar.widthProperty().addListener((observable, oldWidth, newWidth) -> resizePropertyCards(newWidth.doubleValue()));
		resizePropertyCards(propertyBar.getWidth());
	}

	private void initializeDockablePropertyCards() {
		propertyCards.put("brush", brushPropertyCard);
		propertyCards.put("height", heightPropertyCard);
		propertyCards.put("object", objectPropertyCard);
		propertyCards.put("plane", planePropertyCard);
		propertyCards.forEach((key, card) -> {
			card.setId(key + "PropertyCard");
			Number savedWidth = Settings.getSetting(PROPERTY_CARD_WIDTH_PREFIX + key, -1.0);
			if (savedWidth.doubleValue() > 0) {
				card.setPrefWidth(savedWidth.doubleValue());
				customPropertyLayout = true;
			}
			installCardResize(card, key);
			installCardDocking(card, key);
		});

		String savedOrder = Settings.getSetting(PROPERTY_CARD_ORDER, "");
		if (!savedOrder.isBlank()) {
			List<Node> ordered = Arrays.stream(savedOrder.split(","))
				.map(String::trim).map(propertyCards::get).filter(java.util.Objects::nonNull)
				.map(Node.class::cast).toList();
			if (ordered.size() == propertyCards.size()) {
				propertyBar.getChildren().setAll(ordered);
				customPropertyLayout = true;
			}
		}
	}

	private void installCardDocking(VBox card, String key) {
		if (card.getChildren().isEmpty() || !(card.getChildren().get(0) instanceof Label title)) return;
		title.setCursor(Cursor.MOVE);
		title.setTooltip(new Tooltip("Drag to move this panel"));
		title.setOnMousePressed(event -> {
			if (!event.isPrimaryButtonDown()) return;
			draggingCard = card;
			card.getStyleClass().add("dragging-card");
			event.consume();
		});
		title.setOnMouseDragged(event -> {
			if (draggingCard != card) return;
			dockCardAt(event.getSceneX(), event.getSceneY());
			event.consume();
		});
		title.setOnMouseReleased(event -> {
			if (draggingCard != card) return;
			card.getStyleClass().remove("dragging-card");
			propertyBar.getChildren().forEach(node -> node.getStyleClass().remove("dock-target"));
			draggingCard = null;
			savePropertyCardLayout();
			event.consume();
		});
	}

	private void dockCardAt(double sceneX, double sceneY) {
		for (Node node : List.copyOf(propertyBar.getChildren())) {
			if (!(node instanceof VBox target) || target == draggingCard) continue;
			var local = target.sceneToLocal(sceneX, sceneY);
			boolean inside = target.contains(local);
			target.getStyleClass().remove("dock-target");
			if (!inside) continue;
			target.getStyleClass().add("dock-target");
			boolean placeAfter = local.getX() > target.getWidth() / 2;
			propertyBar.getChildren().remove(draggingCard);
			int targetIndex = propertyBar.getChildren().indexOf(target);
			if (placeAfter) targetIndex++;
			propertyBar.getChildren().add(Math.min(targetIndex, propertyBar.getChildren().size()), draggingCard);
			break;
		}
	}

	private void savePropertyCardLayout() {
		customPropertyLayout = true;
		String order = propertyBar.getChildren().stream()
			.map(node -> node.getId().replace("PropertyCard", ""))
			.reduce((left, right) -> left + "," + right).orElse("");
		Settings.putSetting(PROPERTY_CARD_ORDER, order);
		resizePropertyCards(propertyBar.getWidth());
	}

	private void installCardResize(VBox card, String key) {
		card.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
			double localX = card.sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
			if (resizingCard == null) card.setCursor(localX >= card.getWidth() - 8 ? Cursor.H_RESIZE : Cursor.DEFAULT);
		});
		card.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			double localX = card.sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
			if (localX >= card.getWidth() - 8) {
				resizingCard = card;
				resizeStartX = event.getSceneX();
				resizeStartWidth = card.getWidth();
				event.consume();
			}
		});
		card.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
			if (resizingCard == card) {
				double width = Math.max(card.getMinWidth(), Math.min(520, resizeStartWidth + event.getSceneX() - resizeStartX));
				card.setPrefWidth(width);
				customPropertyLayout = true;
				resizePropertyCards(propertyBar.getWidth());
				event.consume();
			}
		});
		card.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
			if (resizingCard == card) {
				Settings.putSetting(PROPERTY_CARD_WIDTH_PREFIX + key, card.getPrefWidth());
				resizingCard = null;
				card.setCursor(Cursor.DEFAULT);
				event.consume();
			}
		});
	}

	private void resizePropertyCards(double width) {
		if (width <= 0) return;
		propertyBar.setPrefWrapLength(width);
		boolean compact = width < 820;
		brushShapeLabel.setVisible(!compact);
		brushShapeLabel.setManaged(!compact);

		double customWidth = propertyBar.getChildren().stream().mapToDouble(node -> ((VBox) node).getPrefWidth()).sum();
		if (customPropertyLayout) {
			setPropertyBarRows(customWidth + 50 <= width ? 1 : width < 430 ? 3 : 2);
			return;
		}
		double usable = Math.max(0, width - 50);
		double minimum = 250 + 185 + 140 + 70;
		if (usable < minimum) {
			brushPropertyCard.setPrefWidth(250);
			heightPropertyCard.setPrefWidth(185);
			objectPropertyCard.setPrefWidth(140);
			planePropertyCard.setPrefWidth(70);
			setPropertyBarRows(width < 430 ? 3 : 2);
			return;
		}
		setPropertyBarRows(1);
		double extra = usable - minimum;
		brushPropertyCard.setPrefWidth(250 + extra * 0.40);
		heightPropertyCard.setPrefWidth(185 + extra * 0.30);
		objectPropertyCard.setPrefWidth(140 + extra * 0.20);
		planePropertyCard.setPrefWidth(70 + extra * 0.10);
	}

	private void setPropertyBarRows(int rows) {
		double height = rows == 1 ? 78 : rows == 2 ? 142 : 206;
		propertyBar.setMinHeight(height);
		propertyBar.setPrefHeight(height);
	}

	public void hideMapEmptyState() {
		mapEmptyState.setVisible(false);
		mapEmptyState.setManaged(false);
	}

	private void deselectTools() {
		toolGroup.getProperties().put("deselect", true);
		toolGroup.selectToggle(null);
	}

	private WindowControls windowControls;

}
