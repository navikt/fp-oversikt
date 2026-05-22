package no.nav.foreldrepenger.oversikt.integrasjoner.brreg;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BrregRollerMapper {

    private static final Map<String, SNRolleType> SN_ROLLER = Arrays.stream(SNRolleType.values())
        .collect(Collectors.toMap(SNRolleType::getKode, Function.identity()));

    private BrregRollerMapper() {
    }


    static BrregSelvstendigNæring mapSelvstendigNæring(BrregRolleutskriftDto.EnhetDto enhet, BrregEnhetDto enhetsdata) {
        var relevanteRoller = rollerForSelvstendigNæringsdrivende(enhet);
        var enhetsinfo = Optional.ofNullable(enhetsdata);
        var virksomhetType = enhetsinfo.map(BrregRollerMapper::utledVirksomhetType).orElse(VirksomhetType.ANNEN);
        var enhetsNavn = enhetsinfo.map(BrregEnhetDto::navn).orElse("Ukjent navn");
        var orgformKode = enhetsinfo.map(BrregEnhetDto::organisasjonsform).map(BrregEnhetDto.EnhetKodeDto::kode).orElse("Ukjent");
        var orgformBeskrivelse = enhetsinfo.map(BrregEnhetDto::organisasjonsform).map(BrregEnhetDto.EnhetKodeDto::kode).orElse("Ukjent");
        return new BrregSelvstendigNæring(enhet.organisasjonsnummer(), enhetsNavn,
            orgformKode, orgformBeskrivelse, virksomhetType,
            enhetsinfo.map(BrregEnhetDto::underAvvikling).orElse(false),
            enhetsinfo.map(BrregEnhetDto::stiftelsesdato).orElse(null),
            enhetsinfo.map(BrregEnhetDto::registreringsdatoEnhetsregisteret).orElse(null),
            relevanteRoller);
    }

    static boolean erSelvstendigNæringsdrivende(BrregRolleutskriftDto.EnhetDto enhet) {
        return !rollerForSelvstendigNæringsdrivende(enhet).isEmpty();
    }

    private static List<SNRolleType> rollerForSelvstendigNæringsdrivende(BrregRolleutskriftDto.EnhetDto enhet) {
        return enhet.roller().stream()
            .map(BrregRolleutskriftDto.RolleDto::type)
            .filter(Objects::nonNull)
            .map(BrregRolleutskriftDto.RolleKodeDto::kode)
            .filter(Objects::nonNull)
            .map(SN_ROLLER::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private static VirksomhetType utledVirksomhetType(BrregEnhetDto enhet) {
        if (enhet.naeringskode1() == null || enhet.naeringskode1().kode() == null) {
            return VirksomhetType.ANNEN;
        }
        var næringskode = enhet.naeringskode1().kode();
        if (næringskode.startsWith("01")) {
            return næringskode.startsWith("01.6") || næringskode.startsWith("01.7") ?
                VirksomhetType.ANNEN : VirksomhetType.JORDBRUK_SKOGBRUK;
        } else if (næringskode.startsWith("02.1")) {
            return VirksomhetType.JORDBRUK_SKOGBRUK;
        } else if (næringskode.startsWith("03.1")) {
            return VirksomhetType.FISKE_FANGST;
        } else if (næringskode.startsWith("88.91")) {
            return VirksomhetType.DAGMAMMA;
        } else {
            return VirksomhetType.ANNEN;
        }
    }



}
