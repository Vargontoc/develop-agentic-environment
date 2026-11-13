package es.vargontoc.agentic.develop.cli.domain.team;

/**
 * Opciones de la sección {@code ## Opciones} de {@code team.md}. Los valores son
 * los identificadores estables que leen las skills; {@code readmeOwner} puede ser null.
 */
public record TeamOptions(String sprints, String gitPolicy, String contractStrategy, String readmeOwner) {

    public static final String SPRINTS_SI = "si";
    public static final String SPRINTS_NO = "no";
    public static final String GIT_BALANCED = "balanced";
    public static final String GIT_STRICT = "strict";
    public static final String CONTRACT_IMPLEMENTATION_FIRST = "implementation-first";
    public static final String CONTRACT_CONTRACT_FIRST = "contract-first";

    public static final TeamOptions DEFAULTS = new TeamOptions(
        SPRINTS_SI, GIT_BALANCED, CONTRACT_IMPLEMENTATION_FIRST, null);

    public static boolean validSprints(String value) {
        return SPRINTS_SI.equals(value) || SPRINTS_NO.equals(value);
    }

    public static boolean validGitPolicy(String value) {
        return GIT_BALANCED.equals(value) || GIT_STRICT.equals(value);
    }

    public static boolean validContractStrategy(String value) {
        return CONTRACT_IMPLEMENTATION_FIRST.equals(value) || CONTRACT_CONTRACT_FIRST.equals(value);
    }

    /** Valor efectivo con los valores por defecto para los campos ausentes. */
    public TeamOptions withDefaults() {
        return new TeamOptions(
            sprints == null || sprints.isBlank() ? DEFAULTS.sprints() : sprints,
            gitPolicy == null || gitPolicy.isBlank() ? DEFAULTS.gitPolicy() : gitPolicy,
            contractStrategy == null || contractStrategy.isBlank()
                ? DEFAULTS.contractStrategy() : contractStrategy,
            readmeOwner == null || readmeOwner.isBlank() ? null : readmeOwner);
    }
}
