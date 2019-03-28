<template>
    <div id="snackbar" v-cloak>
        <v-snackbar v-model="snackbar" :left="true" :timeout="timeout">
            {{ text }}
            <v-btn color="pink" flat @click="snackbar = false">Close</v-btn>
        </v-snackbar>
    </div>
</template>

<script>
export default {
    data () {
        return {
            snackbar: false,
            timeout: 6000,
            text: 'Hello, I\'m a snackbar'
        }
    },
    methods: {
        setVisible(visible, text, timeout) {
            this.snackbar = visible;
            this.text = text;
            if(timeout !== undefined) {
                this.timeout = timeout;
            }
        }
    }
};
</script>
