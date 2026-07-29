<script lang="ts">
	import "../app.css";

	import { ModeWatcher } from "mode-watcher";
	import Header from "$components/Header.svelte";

	// ponytail: no global shortcuts and no updater on mobile — the desktop plugins
	// are not built for Android and APK updates come from GitHub releases.
	let { children } = $props();
</script>

<ModeWatcher />

<Header />
{@render children?.()}
