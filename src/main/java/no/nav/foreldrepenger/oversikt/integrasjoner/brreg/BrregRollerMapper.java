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
        return new BrregSelvstendigNæring(enhet.organisasjonsnummer(), enhetsinfo.map(BrregEnhetDto::navn).orElse(enhet.navn()),
            enhetsinfo.map(BrregEnhetDto::organisasjonsform).map(BrregEnhetDto.EnhetKodeDto::kode).orElse(null),
            enhetsinfo.map(BrregEnhetDto::organisasjonsform).map(BrregEnhetDto.EnhetKodeDto::beskrivelse).orElse(null),
            enhetsinfo.map(BrregRollerMapper::utledVirksomhetstype).orElse(Virksomhetstype.ANNEN),
            enhetsinfo.map(BrregEnhetDto::underAvvikling).orElse(false),
            enhetsinfo.map(BrregEnhetDto::stiftelsesdato).orElse(null),
            enhetsinfo.map(BrregEnhetDto::registreringsdatoEnhetsregisteret).orElse(null),
            relevanteRoller);
    }

    static boolean erSelvstendigNæringsdrivende(BrregRolleutskriftDto.EnhetDto enhet) {
        return !rollerForSelvstendigNæringsdrivende(enhet).isEmpty();
    }

    private static List<SNRolleType> rollerForSelvstendigNæringsdrivende(BrregRolleutskriftDto.EnhetDto enhet) {
        return Optional.ofNullable(enhet.roller()).orElse(List.of()).stream()
            .filter(rolle -> !Boolean.TRUE.equals(rolle.fratraadt()) && !Boolean.TRUE.equals(rolle.avregistrert()))
            .map(BrregRolleutskriftDto.RolleDto::type)
            .filter(Objects::nonNull)
            .map(BrregRolleutskriftDto.RolleKodeDto::kode)
            .filter(Objects::nonNull)
            .map(SN_ROLLER::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private static Virksomhetstype utledVirksomhetstype(BrregEnhetDto enhet) {
        if (enhet.naeringskode1() == null || enhet.naeringskode1().kode() == null) {
            return Virksomhetstype.ANNEN;
        }
        var næringskode = enhet.naeringskode1().kode();
        if (næringskode.startsWith("01")) {
            return næringskode.startsWith("01.6") || næringskode.startsWith("01.7") ?
                Virksomhetstype.ANNEN : Virksomhetstype.JORDBRUK_SKOGBRUK;
        } else if (næringskode.startsWith("02.1")) {
            return Virksomhetstype.JORDBRUK_SKOGBRUK;
        } else if (næringskode.startsWith("03.1")) {
            return Virksomhetstype.FISKE;
        } else if (næringskode.startsWith("88.91")) {
            return Virksomhetstype.DAGMAMMA;
        } else {
            return Virksomhetstype.ANNEN;
        }
    }
}
