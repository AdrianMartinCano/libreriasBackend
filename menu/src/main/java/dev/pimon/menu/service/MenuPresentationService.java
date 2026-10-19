package dev.pimon.menu.service;

import dev.pimon.common.settings.SettingsStore;
import dev.pimon.menu.config.MenuProperties;
import dev.pimon.menu.settings.FontChoice;
import dev.pimon.menu.settings.MenuLayout;
import dev.pimon.menu.settings.MenuPresentation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Presentación de la carta (fuente/color/layout) vigente, cacheada por {@link SettingsStore}. Si no
 * hay fila en BD, deriva los valores por defecto de {@code pimon.menu.*}.
 */
@Service
@RequiredArgsConstructor
public class MenuPresentationService {

    static final String KEY = "menu";

    private final SettingsStore  store;
    private final MenuProperties props;

    public MenuPresentation current() {
        return store.get(KEY, MenuPresentation.class, this::defaults);
    }

    public MenuPresentation update(MenuPresentation presentation) {
        return store.save(KEY, presentation);
    }

    private MenuPresentation defaults() {
        return new MenuPresentation(
                FontChoice.valueOf(props.getHeadingFont()),
                FontChoice.valueOf(props.getBodyFont()),
                props.getAccentColor(),
                MenuLayout.valueOf(props.getLayout()),
                props.isShowPrices(),
                props.isShowAllergens(),
                props.getCurrency());
    }
}
