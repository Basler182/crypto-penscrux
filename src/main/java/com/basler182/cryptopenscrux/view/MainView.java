package com.basler182.cryptopenscrux.view;

import com.basler182.cryptopenscrux.service.CryptoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route("")
@PageTitle("Secure Shamir Wallet")
public class MainView extends VerticalLayout {

    private static final Logger LOG = LoggerFactory.getLogger(MainView.class);
    private final CryptoService cryptoService;

    private record ShamirScheme(String label, int threshold, int totalShares) {
        @NotNull
        @Override
        public String toString() { return label; }
    }

    public MainView(CryptoService cryptoService) {
        this.cryptoService = cryptoService;

        // Root Layout Settings
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        addClassName(LumoUtility.Padding.LARGE);
        addClassName(LumoUtility.Background.CONTRAST_5);

        H1 title = new H1("🔒 Secure Shamir Wallet");
        title.addClassName(LumoUtility.TextColor.HEADER);

        // Navigation Tabs
        Tab createTab = new Tab("Create & Backup");
        Tab recoverTab = new Tab("Recover Wallet");
        Tabs tabs = new Tabs(createTab, recoverTab);
        tabs.setWidthFull();
        tabs.setMaxWidth("800px");

        // Main Content Container
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setMaxWidth("800px");
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName(LumoUtility.Background.BASE);
        content.addClassName(LumoUtility.BoxShadow.SMALL);
        content.addClassName(LumoUtility.BorderRadius.MEDIUM);

        content.add(buildCreateView());

        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            if (event.getSelectedTab().equals(createTab)) {
                content.add(buildCreateView());
            } else {
                content.add(buildRecoverView());
            }
        });

        add(title, tabs, content);
    }

    /**
     * Builds the view for generating a new wallet and splitting the secret.
     */
    private VerticalLayout buildCreateView() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        H3 step1 = new H3("1. Generate Wallet");
        step1.addClassName(LumoUtility.Margin.Top.NONE);

        ComboBox<Integer> wordCountSelect = new ComboBox<>("Word Count");
        wordCountSelect.setItems(12, 20, 24);
        wordCountSelect.setValue(12);

        Button generateBtn = new Button("Generate New Wallet");
        generateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        TextArea mnemonicDisplay = new TextArea("Your Mnemonic Phrase (The Secret)");
        mnemonicDisplay.setReadOnly(true);
        mnemonicDisplay.setWidthFull();
        mnemonicDisplay.setMinHeight("100px");
        mnemonicDisplay.setHelperText("This is your master key. Never share this unencrypted!");

        H3 step2 = new H3("2. Create Shamir Backup");
        ComboBox<ShamirScheme> schemeSelect = new ComboBox<>("Backup Strategy");
        schemeSelect.setItems(
                new ShamirScheme("2 of 3 (Requires 2 parts to recover)", 2, 3),
                new ShamirScheme("3 of 5 (Requires 3 parts to recover)", 3, 5)
        );
        schemeSelect.setValue(schemeSelect.getListDataView().getItem(0));
        schemeSelect.setWidthFull();

        Button splitBtn = new Button("Calculate Backup Shares");
        splitBtn.setEnabled(false);

        VerticalLayout sharesLayout = new VerticalLayout();
        sharesLayout.setPadding(false);
        sharesLayout.setSpacing(true);

        generateBtn.addClickListener(e -> {
            int count = wordCountSelect.getValue();
            List<String> mnemonic = cryptoService.generateMnemonic(count);
            mnemonicDisplay.setValue(String.join(" ", mnemonic));
            splitBtn.setEnabled(true);
            sharesLayout.removeAll();
            showNotification("Wallet generated. Please back it up now!", NotificationVariant.LUMO_SUCCESS);
        });

        splitBtn.addClickListener(e -> {
            sharesLayout.removeAll();
            ShamirScheme scheme = schemeSelect.getValue();
            String secret = mnemonicDisplay.getValue();

            if (secret == null || secret.isEmpty()) return;

            try {
                Map<Integer, String> shares = cryptoService.splitSecret(secret, scheme.threshold(), scheme.totalShares());

                sharesLayout.add(new Hr());
                Span info = new Span("Write these shares down in different secure locations. You will need exactly "
                        + scheme.threshold() + " parts to recover your wallet.");
                info.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SUCCESS);
                sharesLayout.add(info);

                shares.forEach((index, shareString) -> {
                    TextArea shareArea = new TextArea("Share #" + index);
                    shareArea.setValue(shareString);
                    shareArea.setWidthFull();
                    shareArea.setReadOnly(true);

                    Button copyBtn = new Button("Copy");
                    copyBtn.addClickListener(evt -> {
                        copyBtn.getUI().ifPresent(ui ->
                                ui.getPage().executeJs("navigator.clipboard.writeText($0)", shareString));
                        showNotification("Share #" + index + " copied to clipboard", NotificationVariant.LUMO_CONTRAST);
                    });

                    sharesLayout.add(new HorizontalLayout(shareArea, copyBtn));
                });

            } catch (Exception ex) {
                showNotification("Error splitting secret: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
                LOG.error("Shamir Split Error", ex);
            }
        });

        layout.add(step1, new HorizontalLayout(wordCountSelect, generateBtn), mnemonicDisplay,
                new Hr(),
                step2, new HorizontalLayout(schemeSelect, splitBtn), sharesLayout);
        return layout;
    }

    /**
     * Builds the view for reconstructing a wallet from shares.
     */
    private VerticalLayout buildRecoverView() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        H3 title = new H3("Recover Wallet");
        title.addClassName(LumoUtility.Margin.Top.NONE);

        ComboBox<Integer> thresholdSelect = new ComboBox<>("Required Parts (Threshold)");
        thresholdSelect.setItems(2, 3, 4, 5);
        thresholdSelect.setValue(2);
        thresholdSelect.setHelperText("How many shares do you have available?");

        VerticalLayout inputsLayout = new VerticalLayout();
        inputsLayout.setPadding(false);
        inputsLayout.setSpacing(true);

        Button recoverBtn = new Button("Recover Wallet");
        recoverBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        recoverBtn.setWidthFull();

        TextArea resultArea = new TextArea("Recovered Secret");
        resultArea.setWidthFull();
        resultArea.setMinHeight("100px");
        resultArea.setReadOnly(true);

        Runnable updateInputs = () -> {
            inputsLayout.removeAll();
            Integer required = thresholdSelect.getValue();
            if (required == null) return;

            for (int i = 1; i <= required; i++) {
                TextField tf = new TextField("Enter Share #" + i);
                tf.setWidthFull();
                tf.setPlaceholder("Paste Hex string here (e.g., 01A4F...)");
                tf.setHelperText("Format: Index (1 Byte) + Data");
                inputsLayout.add(tf);
            }
            resultArea.clear();
        };

        thresholdSelect.addValueChangeListener(e -> updateInputs.run());
        updateInputs.run(); // Initialize

        recoverBtn.addClickListener(e -> {
            List<String> shareStrings = inputsLayout.getChildren()
                    .filter(c -> c instanceof TextField)
                    .map(c -> ((TextField) c).getValue())
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toList());

            if (shareStrings.size() < thresholdSelect.getValue()) {
                showNotification("Please fill in all " + thresholdSelect.getValue() + " shares.", NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                String recovered = cryptoService.combineShares(shareStrings);
                resultArea.setValue(recovered);
                showNotification("Recovery successful!", NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                resultArea.setValue("");
                showNotification("Recovery Failed: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
                LOG.error("Recovery Error", ex);
            }
        });

        layout.add(title, thresholdSelect, inputsLayout, recoverBtn, new Hr(), resultArea);
        return layout;
    }

    /**
     * Helper to show standardized notifications
     */
    private void showNotification(String text, NotificationVariant variant) {
        Notification notification = Notification.show(text, 4000, Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(variant);
    }
}