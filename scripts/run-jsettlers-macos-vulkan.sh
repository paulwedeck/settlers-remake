#!/usr/bin/env bash
# Run JSettlers with MoltenVK visible to the Vulkan loader (required for Vulkan backends on macOS).
# Install MoltenVK: brew install molten-vk
#
# Optional env vars (set before invoking):
#   JSETTLERS_VK_LOG_LEVEL   MoltenVK log level 0..4 (default 1; 3=debug, 4=verbose)
#   JSETTLERS_VK_VALIDATE    "1" to enable VK_LAYER_KHRONOS_validation if installed
#   JSETTLERS_VK_DEBUG       "1" to print env + ICDs that we picked up
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -z "${VK_ICD_FILENAMES:-}" ]]; then
	MOLTEN_BREW_PREFIX=""
	if command -v brew >/dev/null 2>&1; then
		MOLTEN_BREW_PREFIX="$(brew --prefix molten-vk 2>/dev/null || true)"
	fi
	HB="${HOMEBREW_PREFIX:-/opt/homebrew}"
	for candidate in \
		"${MOLTEN_BREW_PREFIX:+$MOLTEN_BREW_PREFIX/etc/vulkan/icd.d/MoltenVK_icd.json}" \
		"$HB/opt/molten-vk/etc/vulkan/icd.d/MoltenVK_icd.json" \
		"$HB/Cellar/molten-vk"/*/etc/vulkan/icd.d/MoltenVK_icd.json \
		"$HB/share/vulkan/icd.d/MoltenVK_icd.json" \
		"/usr/local/share/vulkan/icd.d/MoltenVK_icd.json" \
		"/usr/local/opt/molten-vk/etc/vulkan/icd.d/MoltenVK_icd.json"; do
		if [[ -f "$candidate" ]]; then
			export VK_ICD_FILENAMES="$candidate"
			break
		fi
	done
fi

# MoltenVK verbose logs - they print [mvk-info]/[mvk-warn]/[mvk-error] markers to stderr.
# Level 4 (verbose) prints every Metal encoder open/close which is invaluable for triaging
# black-screen issues, but it's also extremely chatty - default to 1 (errors only).
export MVK_CONFIG_LOG_LEVEL="${MVK_CONFIG_LOG_LEVEL:-${JSETTLERS_VK_LOG_LEVEL:-1}}"
# Make MoltenVK fail fast on programming errors instead of silently returning success.
export MVK_CONFIG_DEBUG="${MVK_CONFIG_DEBUG:-1}"
# Trace API calls if debug is on - level >=3 includes per-call traces.
if [[ "${MVK_CONFIG_LOG_LEVEL}" -ge 3 ]]; then
	export MVK_CONFIG_TRACE_VULKAN_CALLS="${MVK_CONFIG_TRACE_VULKAN_CALLS:-1}"
fi
# MoltenVK 1.2.10+ enables Metal argument buffers by default, which produces black-screen
# rendering on Apple Silicon for several engines (notably Dolphin). JSettlers' descriptor
# layout (combined image-samplers + uniform buffers per-pipeline) hits the same code path.
# Disabling argument buffers makes MoltenVK fall back to inline argument tables, which is
# slower but actually renders pixels. Set MVK_USE_METAL_ARGUMENT_BUFFERS=1 to opt back in.
#
# Note: VulkanUtils now passes the same flag via VK_EXT_layer_settings during vkCreateInstance,
# so this env var is redundant on MoltenVK >= 1.2.7. We keep it as a belt-and-braces fallback
# for users on older MoltenVK builds where the extension is unavailable.
if [[ -z "${MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS:-}" ]]; then
	export MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS=0
fi

# If the user installed the Vulkan SDK and asked for validation, enable it. The
# validation layer is the single best way to catch silent rendering bugs (wrong
# image layouts, missing barriers, mismatched descriptor sets, etc.) and the SDK
# install puts it under $VULKAN_SDK/share/vulkan/explicit_layer.d/.
if [[ "${JSETTLERS_VK_VALIDATE:-}" == "1" ]]; then
	if [[ -n "${VULKAN_SDK:-}" && -d "$VULKAN_SDK/share/vulkan/explicit_layer.d" ]]; then
		export VK_LAYER_PATH="$VULKAN_SDK/share/vulkan/explicit_layer.d"
		export VK_INSTANCE_LAYERS="VK_LAYER_KHRONOS_validation"
		echo "[validation] VK_LAYER_PATH=$VK_LAYER_PATH"
		echo "[validation] VK_INSTANCE_LAYERS=$VK_INSTANCE_LAYERS"
	else
		echo "[validation] VULKAN_SDK not set or no explicit_layer.d - skipping validation"
	fi
fi

cd "$ROOT"

if [[ -n "${VK_ICD_FILENAMES:-}" ]]; then
	echo "[moltenvk] VK_ICD_FILENAMES=$VK_ICD_FILENAMES"
fi
if [[ -n "${JSETTLERS_VK_DEBUG:-}" ]]; then
	echo "[moltenvk] MVK_CONFIG_LOG_LEVEL=${MVK_CONFIG_LOG_LEVEL}"
	echo "[moltenvk] MVK_CONFIG_DEBUG=${MVK_CONFIG_DEBUG}"
	echo "[moltenvk] MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS=${MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS}"
fi

exec ./gradlew run "$@"
