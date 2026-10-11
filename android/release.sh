#!/bin/zsh
# Crea il pacchetto firmato per Google Play: android/release/Receipty-<versione>.aab
#
# La prima volta crea anche la chiave di caricamento (~/Documents/receipty-upload.jks):
# va conservata, insieme alla sua password, per tutti gli aggiornamenti futuri.
# Uso: ./release.sh
set -e
cd "$(dirname "$0")"
KEY=${RECEIPTY_KEY:-$HOME/Documents/receipty-upload.jks}
export JAVA_HOME=${JAVA_HOME:-"/Applications/Android Studio.app/Contents/jbr/Contents/Home"}
VERSION=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)

if [[ ! -f $KEY ]]; then
  echo "Prima volta: creo la chiave di caricamento in $KEY."
  echo "Inventa una password (almeno 6 caratteri) e segnatela: serve per ogni aggiornamento."
  while true; do
    read -rs "PASS?Password: "; echo
    read -rs "CONFIRM?Ripetila: "; echo
    [[ $PASS == "$CONFIRM" && ${#PASS} -ge 6 ]] && break
    echo "Non coincidono o è troppo corta, riprova."
  done
  unset CONFIRM
  export RECEIPTY_PASS=$PASS
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$KEY" -alias upload \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Receipty" \
    -storepass:env RECEIPTY_PASS -keypass:env RECEIPTY_PASS 2>&1 | grep -v "^Generating" || true
  unset RECEIPTY_PASS
  [[ -f $KEY ]] || { echo "Non sono riuscita a creare la chiave." >&2; exit 1; }
else
  read -rs "PASS?Password della chiave: "; echo
fi

echo "Creo il pacchetto firmato (un paio di minuti)…"
./gradlew :app:bundleRelease -q --console=plain \
  -Pandroid.injected.signing.store.file="$KEY" \
  -Pandroid.injected.signing.store.password="$PASS" \
  -Pandroid.injected.signing.key.alias=upload \
  -Pandroid.injected.signing.key.password="$PASS"
unset PASS

# La build può stare fuori dal Desktop (vedi build.gradle.kts): si prende il pacchetto appena fatto.
AAB=$(ls -t "$HOME/.gradle-builds/Receipty/app/outputs/bundle/release/app-release.aab" \
            app/build/outputs/bundle/release/app-release.aab 2>/dev/null | head -1)
mkdir -p release
cp "$AAB" "release/Receipty-$VERSION.aab"
if ! "$JAVA_HOME/bin/jarsigner" -verify "release/Receipty-$VERSION.aab" | grep -q "jar verified"; then
  echo "Il pacchetto non risulta firmato: qualcosa è andato storto." >&2
  exit 1
fi
echo "Fatto: $(pwd)/release/Receipty-$VERSION.aab"
open -R "release/Receipty-$VERSION.aab"
