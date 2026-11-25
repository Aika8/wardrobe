#!/bin/bash
# Toolchain for cloud sessions of this repository (docs/backlog/README.md, "Environment").
#
# Runs by itself: `.claude/settings.json` calls it as a SessionStart hook at the start of every cloud session, so
# nothing has to be configured on claude.ai. It does nothing outside the cloud (CLAUDE_CODE_REMOTE != true), and is
# idempotent: a second run on the same VM finds everything in place and finishes in about a second. The prompts of
# the backlog runs call it again as their first step, in case the hook did not run.
# It can also be pasted as the environment's Setup script on claude.ai: then the result is cached between sessions.
#
# What the project needs and the image lacks: JDK 25 (image has 21), Node 24 (image has 20–22; the mobile tests run
# .ts files with `node --test`, which needs Node ≥ 23.6). Copied from Aika8/learning-exchange without Godot.
# Sources are on the default allowlist of a Limited environment (cloud-environments docs, "Default allowed domains"):
#   JDK 25   download.oracle.com (Oracle JDK «latest», NFTC licence) → fallback: Ubuntu's openjdk-25 (archive.ubuntu.com)
#   Node 24  registry.npmjs.org (package node-linux-x64, the official build repackaged) → fallback: nodejs.org,
#            reachable only if added to the environment's allowed domains
# Gradle (services.gradle.org) and Maven Central are on the allowlist; the Gradle toolchain is pointed at the
# installed JDK, so foojay (api.foojay.io, not on the allowlist) is never asked.
set -u

# As a hook: only in the cloud. As a setup script (no CLAUDE_PROJECT_DIR yet): always.
if [ -n "${CLAUDE_PROJECT_DIR:-}" ] && [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi
[ "$(uname -s)" = "Linux" ] || exit 0

LOG=/tmp/setup-env.log
SUDO=""; [ "$(id -u)" -ne 0 ] && command -v sudo >/dev/null && SUDO="sudo"

summary() {
  echo "setup-env: java $(/opt/jdk-25/bin/java -version 2>&1 | head -1 | cut -d'"' -f2 || echo MISSING)," \
       "node $(/opt/node24/bin/node --version 2>/dev/null || echo MISSING) (log $LOG)"
}

# Commits made in the cloud carry Aika's name and her GitHub no-reply address, not an AI identity
# (CLAUDE.md: no AI attribution). The attribution trailers themselves are off in .claude/settings.json.
GIT_NAME="Aika"
GIT_EMAIL="52049894+Aika8@users.noreply.github.com"

export_env() {
  # Later Bash commands of the session: CLAUDE_ENV_FILE (SessionStart hooks), profile.d, top of ~/.bashrc.
  git config --global user.name "$GIT_NAME" 2>/dev/null || true
  git config --global user.email "$GIT_EMAIL" 2>/dev/null || true
  if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    { echo 'export JAVA_HOME=/opt/jdk-25'; echo 'export PATH=/opt/node24/bin:/opt/jdk-25/bin:$PATH'
      echo "export GIT_AUTHOR_NAME='$GIT_NAME' GIT_AUTHOR_EMAIL='$GIT_EMAIL'"
      echo "export GIT_COMMITTER_NAME='$GIT_NAME' GIT_COMMITTER_EMAIL='$GIT_EMAIL'"; } >> "$CLAUDE_ENV_FILE"
  fi
}

if [ -x /opt/jdk-25/bin/java ] && [ -x /opt/node24/bin/node ] && [ -f "$HOME/.gradle/gradle.properties" ]; then
  export_env; summary; exit 0
fi

{
echo "setup-env: start $(date -u +%FT%TZ)"

install_jdk() {
  local dir=/opt/jdk-25
  [ -x "$dir/bin/java" ] && return 0
  local tmp; tmp=$(mktemp -d)
  if curl -fsSL --retry 3 --max-time 200 -o "$tmp/jdk.tgz" https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz; then
    $SUDO mkdir -p "$dir" && $SUDO tar -xzf "$tmp/jdk.tgz" -C "$dir" --strip-components=1
  else
    echo "setup-env: Oracle JDK download failed, trying apt openjdk-25"
    $SUDO apt-get update -qq && $SUDO apt-get install -y -qq openjdk-25-jdk-headless || true
    local found; found=$(ls -d /usr/lib/jvm/*25* 2>/dev/null | head -1)
    [ -n "$found" ] && $SUDO ln -sfn "$found" "$dir"
  fi
  rm -rf "$tmp"
}

install_node() {
  local dir=/opt/node24
  [ -x "$dir/bin/node" ] && return 0
  local tmp; tmp=$(mktemp -d)
  if (cd "$tmp" && npm pack --silent node-linux-x64@24 >/dev/null 2>&1) && ls "$tmp"/node-linux-x64-*.tgz >/dev/null 2>&1; then
    $SUDO mkdir -p "$dir" && $SUDO tar -xzf "$tmp"/node-linux-x64-*.tgz -C "$dir" --strip-components=1
  fi
  if [ ! -x "$dir/bin/node" ]; then
    echo "setup-env: npm registry failed, trying nodejs.org"
    local ver
    ver=$(curl -fsSL --max-time 20 https://nodejs.org/dist/index.json 2>/dev/null \
          | python3 -c 'import json,sys; print(next(r["version"] for r in json.load(sys.stdin) if r["version"].startswith("v24.")))' 2>/dev/null)
    if [ -n "$ver" ] && curl -fsSL --retry 3 --max-time 120 -o "$tmp/node.txz" "https://nodejs.org/dist/$ver/node-$ver-linux-x64.tar.xz"; then
      $SUDO mkdir -p "$dir" && $SUDO tar -xJf "$tmp/node.txz" -C "$dir" --strip-components=1
    fi
  fi
  # The npm-registry build may come without npm: use the image's npm with Node 24.
  if [ -x "$dir/bin/node" ] && [ ! -x "$dir/bin/npm" ] && [ -d /opt/node22/lib/node_modules/npm ]; then
    $SUDO mkdir -p "$dir/lib/node_modules" && $SUDO cp -r /opt/node22/lib/node_modules/npm "$dir/lib/node_modules/"
    $SUDO ln -sfn ../lib/node_modules/npm/bin/npm-cli.js "$dir/bin/npm"
    $SUDO ln -sfn ../lib/node_modules/npm/bin/npx-cli.js "$dir/bin/npx"
  fi
  rm -rf "$tmp"
}

install_jdk & J=$!
install_node & N=$!
wait $J || true
wait $N || true

$SUDO tee /etc/profile.d/wr-toolchain.sh >/dev/null <<'EOF'
export JAVA_HOME=/opt/jdk-25
export PATH=/opt/node24/bin:/opt/jdk-25/bin:$PATH
export GIT_AUTHOR_NAME="Aika" GIT_AUTHOR_EMAIL="52049894+Aika8@users.noreply.github.com"
export GIT_COMMITTER_NAME="Aika" GIT_COMMITTER_EMAIL="52049894+Aika8@users.noreply.github.com"
EOF
# At the TOP of ~/.bashrc: Ubuntu's .bashrc returns early for non-interactive shells.
if ! grep -q wr-toolchain "$HOME/.bashrc" 2>/dev/null; then
  { echo '. /etc/profile.d/wr-toolchain.sh  # wr-toolchain'; cat "$HOME/.bashrc" 2>/dev/null; } > /tmp/bashrc.new \
    && mv /tmp/bashrc.new "$HOME/.bashrc"
fi
for b in node npm npx; do [ -x "/opt/node24/bin/$b" ] && $SUDO ln -sfn "/opt/node24/bin/$b" "/usr/local/bin/$b"; done
for b in java javac jar; do [ -x "/opt/jdk-25/bin/$b" ] && $SUDO ln -sfn "/opt/jdk-25/bin/$b" "/usr/local/bin/$b"; done

# Gradle: use the installed JDK 25, never auto-download (foojay is not reachable).
mkdir -p "$HOME/.gradle"
cat > "$HOME/.gradle/gradle.properties" <<'EOF'
org.gradle.java.installations.paths=/opt/jdk-25
org.gradle.java.installations.auto-download=false
EOF
echo "setup-env: done $(date -u +%FT%TZ)"
} >> "$LOG" 2>&1

export_env
summary
exit 0
