<template>
    <div id="create-dialog" v-cloak>
        <v-app id="inspire">
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
                                            <v-text-field outline label="Name*" v-model="name" :rules="nameRules" :counter="50" required></v-text-field>
                                        </v-flex>
                                        <v-flex xs12>
                                            <v-text-field outline append-icon="link" label="Location*" v-model="location" :rules="locationRules" :counter="2083" required autocomplete="url"></v-text-field>
                                        </v-flex>
                                        <v-flex xs12>
                                            <v-select v-if="edit === false" v-model="select" :items="items" :rules="[v => !!v || 'Type is required.']" label="Type" required></v-select>
                                        </v-flex>
                                    </v-layout>
                                </v-container>
                                <small>*indicates required field</small>
                            </v-card-text>
                            <v-card-actions>
                                <v-btn color="pink" flat @click="clear">Clear</v-btn>
                                <v-spacer></v-spacer>
                                <v-btn color="pink" flat @click="dialog = false">Cancel</v-btn>
                                <v-btn color="pink" flat :disabled="!valid" @click="edit ? update() : submit()">{{ action }}</v-btn>
                            </v-card-actions>
                        </v-card>
                    </v-form>
                </v-dialog>
            </v-layout>
        </v-app>
    </div>
</template>
<script>
import axios from "axios";
import { storageUser, storageToken } from "../../var.js";

const urlRegex = new RegExp('https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)');
const nameRegex= new RegExp('^[a-zA-Z0-9_.-]*$');
export default {
    data () {
        return {
            dialog: false,
            valid: false,
            edit: false,
            title: 'New jump point',
            action: 'Create',
            id: -1,
            name: '',
            nameRules: [
                (v) => !!v || 'This is a required field.',
                (v) => nameRegex.exec(v) || 'Name must not contain special characters',
                (v) => v && v.length < 50 || 'Name must be less than 50 characters'
            ],
            location: '',
            locationRules: [
                (v) => !!v || 'This is a required field.',
                (v) => urlRegex.exec(v) || 'URL must be valid.',
                (v) => v && v.length < 2083 || 'Location must be less than 2083 characters'
            ],
            select: null,
            items: [
                "Global",
                "Personal"
            ],
            index: -1
        }
    },
    methods: {
        setVisible(visible, title, action, edit, id, name, location, index) {
            if(edit)
                this.edit = edit;
            else {
                this.edit = false;
                this.$refs.form.reset();
            }
            this.dialog = visible;
            this.title = title;
            this.action = action;
            if(id) {
                this.id = id;
            }
            if(name) {
                this.name = name;
            }
            if(location)
                this.location = location;
            if(index !== undefined)
                this.index = index;
            else
                this.index = -1;
        },
        update () {
            this.$refs.form.validate();
            let url = `${process.env.VUE_APP_BASE_URL}/v1/jumps/edit`;
            let that = this;
            axios.patch(
                url,
                `{ "id": ${this.id}, "name": "${this.name}", "location": "${this.location}" }`,
                {headers: {"Content-Type": "application/json", "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                console.log(that.index);
                console.log(this.index);
                this.$emit('jumpsSetItem', { name: this.name, location: this.location, personal: this.select === this.items[1] }, that.index);
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
                this.$emit('snackbar', true, `Updated ${that.name}`);
            }).catch(e => {
                console.log(e);
                this.$emit('snackbar', true, `Failed to update: ${e.response.status}`);
            });
        },
        submit () {
            this.$refs.form.validate();
            let url = `${process.env.VUE_APP_BASE_URL}/v1/jumps/add`;
            const localToken = localStorage.getItem(storageToken);
            let personalJump = this.select === this.items[1];
            if(localToken === null && personalJump === true) {
                // User cannot create personal tokens if not auth'd
                this.$emit('snackbar', true, "Login to create personal jumps!");
                return;
            }
            let that = this;
            axios.put(
                url,
                `{ "name": "${this.name}", "location": "${this.location}", "personal": "${personalJump}" }`,
                {headers: {"Content-Type": "application/json", "X-Auth-Token": localToken, "X-Auth-User": localStorage.getItem(storageUser)}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                this.$emit('jumpsPushItem', {
                    name: that.name,
                    location: that.location,
                    personal: personalJump
                });
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
                this.$emit('snackbar', true, `Added ${that.name}`)
            }).catch(e => {
                console.log(e);
                this.$emit('snackbar', true, `Failed to add: ${e.response.status}`);
            });
        },
        clear () {
            this.name = '';
            this.location = '';
            this.$refs.form.reset();
        }
    }
};
</script>
