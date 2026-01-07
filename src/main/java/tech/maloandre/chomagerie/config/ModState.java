package tech.maloandre.chomagerie.config;

/**
 * Classe de configuration partagée pour stocker l'état d'activation du mod
 * Permet au serveur de savoir si le client a désactivé le mod
 */
public class ModState {
    private static boolean clientEnabled = true;

    public static boolean isClientEnabled() {
        return clientEnabled;
    }

    public static void setClientEnabled(boolean enabled) {
        clientEnabled = enabled;
    }
}

