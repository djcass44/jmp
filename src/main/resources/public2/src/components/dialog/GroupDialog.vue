<template>
    <div id="auth-dialog" v-cloak>
        <v-layout row justify-center>
            <v-dialog v-model="dialog" persistent max-width="600px">
                <v-form v-model="valid" ref="form">
                    <v-card>
                        <v-card-title>
                            <span class="strong-title headline">{{ title }}</span>
                        </v-card-title>
                        <v-card-text>
                            <v-container grid-list-md>
                                <v-layout wrap>
                                    <v-flex xs12>
                                        <v-text-field outline label="Name*" v-model="name" :rules="nameRules" :counter="24" required autocomplete="name"></v-text-field>
                                    </v-flex>
                                </v-layout>
                            </v-container>
                            <small>*indicates required field</small>
                        </v-card-text>
                        <v-card-actions>
                            <v-spacer></v-spacer>
                            <v-btn color="pink" flat @click="dialog = false">Cancel</v-btn>
                            <v-btn color="pink" flat :disabled="valid === false || loading === true" @click="create ? onCreate() : onUpdate()">{{ action }}</v-btn>
                        </v-card-actions>
                    </v-card>
                </v-form>
            </v-dialog>
        </v-layout>
    </div>
</template>
<script>
import axios from "axios";
import { storageJWT } from "../../var.js";

const nameRegex= new RegExp('^[a-zA-Z0-9_.-]*$');
export default {
    data () {
        return {
            loading: false,
            dialog: false,
            valid: false,
            name: '',
            nameRules: [
                (v) => !!v || 'This is a required field.',
                (v) => nameRegex.test(v) || 'Username must not contain special characters',
                (v) => v && v.length < 25 || 'Username must be less than 25 characters'
            ],
            create: true,
            title: '',
            action: '',
            item: null
        }
    },
    methods: {
        setVisible(visible, create, item) {
            this.dialog = visible;
            if(visible === true)
                this.$refs.form.reset();
            if(create === true) {
                this.title = "Add a new group";
                this.action = "Create";
            }
            else {
                this.title = "Edit group";
                this.action = "Update";
            }
            if(item) {
                this.item = item;
                if(create === false)
                    this.name = item.name;
            }
            this.create = create;
        },
        onCreate() {
            this.loading = true;
            this.$refs.form.validate();
            const url = `${process.env.VUE_APP_BASE_URL}/v2_1/group/add`;
            let that = this;
            axios.put(url, `{ "name": "${this.name}" }`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.dialog = false;
                that.$emit("pushItem");
                that.loading = false;
                that.$emit('snackbar', true, `Created group ${that.name}`);
            }).catch(e => {
                console.log(e);
                that.loading = false;
                that.$emit('snackbar', true, `Failed to create group: ${e.response.status}`);
            });
        },
        onUpdate() {
            this.loading = true;
            this.$refs.form.validate();
            let that = this;
            // Update the name before converting to JSON
            that.item.name = that.name;
            axios.patch(`${process.env.VUE_APP_BASE_URL}/v2_1/group/edit`, JSON.stringify(that.item),{ headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.dialog = false;
                that.$emit("pushItem");
                that.loading = false;
                that.$emit('snackbar', true, `Updated group ${that.name}`);
            }).catch(e => {
                console.log(e);
                that.loading = false;
                that.$emit('snackbar', true, `Failed to update group: ${e.response.status}`);
            });
        }
    }
};
</script>
