package no.nav.foreldrepenger.oversikt.domene.svp;

import java.util.Set;

public enum ResultatÅrsak {

    INNVILGET,
    AVSLAG_SØKNADSFRIST,
    AVSLAG_ANNET,
    AVSLAG_INNGANGSVILKÅR,
    OPPHØR_OVERGANG_FORELDREPENGER,
    OPPHØR_FØDSEL,
    OPPHØR_TIDSPERIODE_FØR_TERMIN,
    OPPHØR_OPPHOLD_I_YTELSEN,
    OPPHØR_ANNET;

    public boolean erOpphør() {
        return Set.of(OPPHØR_ANNET, OPPHØR_FØDSEL, OPPHØR_OVERGANG_FORELDREPENGER, OPPHØR_OPPHOLD_I_YTELSEN,
            OPPHØR_TIDSPERIODE_FØR_TERMIN).contains(this);
    }
}
