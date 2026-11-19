#!/bin/sh
set -eu

repo="mythicisland/queue"
version="${QUEUE_VERSION:-latest}"
install_dir="${QUEUE_INSTALL_DIR:-/usr/local/bin}"

fail() {
  echo "$1" >&2
  exit 1
}

case "$(uname -s)" in
  Linux) os=linux ;;
  Darwin) os=darwin ;;
  *) fail "Unsupported operating system: $(uname -s)" ;;
esac

case "$(uname -m)" in
  x86_64 | amd64) arch=amd64 ;;
  arm64 | aarch64) arch=arm64 ;;
  *) fail "Unsupported architecture: $(uname -m)" ;;
esac

if [ "$version" = "latest" ]; then
  base="https://github.com/$repo/releases/latest/download"
else
  base="https://github.com/$repo/releases/download/$version"
fi

archive="queue-cli_${os}_${arch}.tar.gz"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

echo "Downloading $archive ($version)"
curl -fsSL "$base/$archive" -o "$tmp/$archive"
curl -fsSL "$base/queue-cli_checksums.txt" -o "$tmp/checksums.txt"

expected="$(grep " $archive\$" "$tmp/checksums.txt" | cut -d ' ' -f 1)"
if command -v sha256sum >/dev/null 2>&1; then
  actual="$(sha256sum "$tmp/$archive" | cut -d ' ' -f 1)"
else
  actual="$(shasum -a 256 "$tmp/$archive" | cut -d ' ' -f 1)"
fi
if [ -z "$expected" ] || [ "$expected" != "$actual" ]; then
  fail "Checksum of $archive does not match"
fi

tar -xzf "$tmp/$archive" -C "$tmp" queue
chmod +x "$tmp/queue"

mkdir -p "$install_dir" 2>/dev/null || true
if [ -w "$install_dir" ]; then
  mv "$tmp/queue" "$install_dir/queue"
else
  echo "Installing to $install_dir needs sudo"
  sudo mkdir -p "$install_dir"
  sudo mv "$tmp/queue" "$install_dir/queue"
fi

echo "Installed queue to $install_dir/queue"
case ":$PATH:" in
  *":$install_dir:"*) ;;
  *) echo "Add $install_dir to your PATH to run queue from anywhere" ;;
esac
