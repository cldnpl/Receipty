# Receipty — App Store Connect

Tutti i testi della scheda, già dentro i limiti di caratteri. Lingua principale: English (U.S.).

L'app è tradotta in 12 lingue (vedi `Resources/*.lproj`). In App Store Connect la scheda resta
solo in inglese: aggiungere le localizzazioni della scheda è un lavoro a parte, quando serve.

## App Information

| Campo | Valore |
|---|---|
| Name (max 30) | `Receipty: Split the Bill` (24) |
| Subtitle (max 30) | `Scan receipts, see who owes` (27) |
| Primary category | Finance |
| Secondary category | Food & Drink |
| Content rights | Does not contain, show, or access third-party content |
| Privacy Policy URL | https://github.com/cldnpl/Receipty/blob/main/PRIVACY_POLICY.md |
| License agreement | Apple's Standard EULA (il nostro `EULA.md` la richiama) |

## Version 1.1

La 1.0 è già approvata: il suo treno è chiuso e App Store Connect rifiuta altre build con quel
numero. Da qui in avanti ogni rilascio alza `MARKETING_VERSION` in `project.yml`.

### What's New in This Version (max 4000, 362)

```
Receipty now speaks your language.

• 12 languages: English, Italian, Spanish, French, German, Portuguese, Dutch, Turkish, Russian, Japanese, Korean and Simplified Chinese.
• Pick one in Settings and the app changes on the spot, whatever your iPhone is set to.
• Amounts, dates and currency names follow the language too, so the total reads the way you write it.
```

### Promotional text (max 170, 157)

```
Dinner's over and the check just landed? Scan it, tap who had what, and Receipty shows exactly who pays whom. Paid with a big note? The change is sorted too.
```

### Description (max 4000, 1923)

```
The check lands and everyone reaches for the calculator. Receipty ends that.

Scan the receipt, tap who had what, enter who actually paid, and see exactly who owes whom, with the fewest possible transfers.

SCAN THE RECEIPT
• Point the camera at the receipt or pick a photo. Receipty reads the items and prices right on your iPhone.
• Anything it isn't sure about is highlighted, so you check it instead of trusting a guess.
• Fix names, prices and quantities with a tap, or add items by hand.

TAP WHO HAD WHAT
• Tap the names on each item. Shared a pizza? Tap both and the price is split automatically.
• "All" puts an item on everyone: perfect for the cover charge or a bottle for the table.
• A running total shows what each person has had so far.

OR SPLIT IT EQUALLY
No receipt? Type the total and who paid. Receipty splits it evenly, down to the cent.

WHO PAYS WHOM
• Enter what each person put down. Receipty works out the simplest way to settle up: the minimum number of transfers.
• Paid with a big note? Receipty tells you who keeps the change and what everyone still owes afterwards.
• "How we got here" shows each person's share, what they paid and the difference.
• Copy a summary and paste it into the group chat.

MADE FOR THE TABLE
• Fast, clear and easy to use with one hand, even while the waiter waits.
• 68 currencies, including ones without decimals like yen and won.
• 12 languages: English, Italian, Spanish, French, German, Portuguese, Dutch, Turkish, Russian, Japanese, Korean and Simplified Chinese. Pick one in Settings, whatever your iPhone is set to.
• Light and dark mode.
• Recent bills are kept so you can check them later. Swipe to delete.

PRIVATE BY DESIGN
No account, no ads, no tracking. Receipts are read on your iPhone, and photos are never uploaded or saved. Your bills stay on your device.

Receipty is a calculator: it tells you who owes what, and you settle up however you like.
```

### Keywords (max 100, 100)

Senza spazi e senza ripetere parole già nel nome o nel sottotitolo (Apple le conta già).

```
splitter,check,tab,restaurant,dinner,friends,group,expense,calculator,share,divide,settle,change,ocr
```

### URLs

| Campo | Valore |
|---|---|
| Support URL | https://github.com/cldnpl/Receipty |
| Marketing URL | (vuoto) |

### Copyright

```
2026 Claudia Napolitano
```

### App Review Information

Sign-in required: **No**. Contact: i tuoi dati.

Notes (701 / 4000):

```
Receipty needs no account or sign-in and works offline. The only web pages are the Privacy Policy and Terms of Use, which open in Safari from the Settings tab.

Quick test without a receipt:
1. On the Bills screen tap +, then "Enter manually".
2. Add four names (e.g. Anna, Ben, Cara, Dan) and tap "Who paid?".
3. Enter 52 as the bill total, 20 for Anna and 40 for Ben, then tap Calculate. The result shows who keeps the 8.00 change and who pays whom.

Receipt scanning: tap +, then "Scan receipt". The camera is used only to read the receipt on the device (Apple Vision); the photo is not stored or uploaded. You can also choose a photo from the library, or tap "Continue manually" to type the items.
```

## App Privacy

**Data Not Collected.** L'app non raccoglie né invia dati: conti, nomi e impostazioni restano sul telefono, lo scontrino si legge sul dispositivo e la foto non si salva. Nessun SDK di terze parti, nessun tracciamento.

## Age Rating

Rispondi **None / No** a tutto (niente contenuti sensibili, niente web browsing libero, niente contenuti generati da utenti condivisi, niente acquisti): risultato **4+**.

## Export compliance

Già dichiarato nel progetto (`ITSAppUsesNonExemptEncryption = NO`): App Store Connect non chiede nulla.
