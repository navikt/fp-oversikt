alter index sak_saksnummer_key rename to "uidx_sak_saksnummer";
comment on table sak is 'Inneholder saker til bruker';
comment on column sak.saksnummer is 'Saksnummer til sak';
comment on column sak.json is 'Inneholder en json representasjon av saken';

alter index tilbakekreving_saksnummer_key rename to "uidx_tilbakekreving_saksnummer";
comment on table tilbakekreving is 'Inneholder tilbakekreving saker';
comment on column tilbakekreving.saksnummer is 'Saksnummer til sak';
comment on column tilbakekreving.json is 'Inneholder en json representasjon av tilbakekrevingsaken';

alter index manglende_vedlegg_saksnummer_key rename to "uidx_manglende_vedlegg_saksnummer";
comment on table manglende_vedlegg is 'Inneholder oversikt over manglende vedlegg for gitt saks';
comment on column manglende_vedlegg.saksnummer is 'Saksnummer knyttet til manglende vedlegg';
comment on column manglende_vedlegg.json is 'Inneholder en json representasjon av manglende vedlegg';

alter index inntektsmeldinger_saksnummer_key rename to "uidx_inntektsmeldinger_saksnummer";
comment on table inntektsmeldinger is 'Inneholder innteksmeldinger til bruker';
comment on column inntektsmeldinger.saksnummer is 'Saksnummer knyttet til innteksmeldingen';
comment on column inntektsmeldinger.json is 'Inneholder en json representasjon av innteksmeldingen';

comment on table oppgave is 'Inneholder oppgaver til bruker';
comment on column oppgave.saksnummer is 'Saksnummer knyttet til oppgaven';
comment on column oppgave.type is 'Hvilken type oppgave det gjelder';
comment on column oppgave.avsluttet_tid is 'Tidspunktet oppgaven ble avsluttet';




