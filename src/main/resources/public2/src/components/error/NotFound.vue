<template>
    <div>
        <div class="mdl-grid">
            <div class="mdl-layout-spacer"></div>
            <h1 class="mdl-h1">404</h1>
            <div class="mdl-layout-spacer"></div>
        </div>
        <div class="mdl-grid">
            <div class="mdl-layout-spacer"></div>
            <p class="mdl-h5">The page you're looking for couldn't be found.</p>
            <div class="mdl-layout-spacer"></div>
        </div>
    </div>
</template>
<script>
export default {
    name: 'NotFound',
    methods: {
        setLoggedIn() {},
        authChanged() {}
    }
}
</script>
<style scoped>
.mdl-h1 {
    color: #616161;
    font-size: 148px;
    font-weight: 300;
}
.mdl-h5 {
    color: #757575;
    font-size: 32px;
    font-weight: 400;
}
</style>
