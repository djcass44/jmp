<template>
    <v-container grid-list-md text-xs-center>
        <v-layout row wrap>
            <v-flex xs12><h1 class="mdl-h1">404</h1></v-flex>
            <v-flex xs12><p class="mdl-h5">The page you're looking for doesn't exist.</p></v-flex>
            <v-flex xs12><v-btn icon @click="openHome"><v-icon color="primary">home</v-icon></v-btn></v-flex>
        </v-layout>
    </v-container>
</template>
<script>
export default {
    name: 'NotFound',
    methods: {
        setLoggedIn() {},
        authChanged() {},
        openHome: function(event) {
            window.location.href = process.env.VUE_APP_FE_URL;
        },
    },
    mounted: function() {
        this.$emit('postInit');
    }
}
</script>
<style scoped>
.mdl-h1 {
    color: #616161;
    font-size: 148px;
    font-weight: 200;
}
.mdl-h5 {
    color: #656565;
    font-size: 28px;
    font-weight: 400;
}
</style>
