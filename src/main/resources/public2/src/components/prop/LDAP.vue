<template>
    <div id="main-list" v-cloak v-if="init === true">
        <v-subheader inset>Authentication</v-subheader>
        <v-expansion-panel>
            <v-expansion-panel-content>
              <template v-slot:header>
                <div>LDAP (read-only)</div>
              </template>
              <v-card>
                <v-card-text>
                    <v-layout>
                        <v-flex xs12>
                            <div v-for="p in props">
                                <p>{{ p.name }}={{ p.value}}</p>
                            </div>
                        </v-flex>
                    </v-layout>
                </v-card-text>
              </v-card>
            </v-expansion-panel-content>
        </v-expansion-panel>
    </div>
</template>
<script>
import axios from "axios";
import { storageJWT } from "../../var.js";

export default {
    name: 'LDAP',
    data() {
        return {
            init: false,
            props: [
                {
                    name: 'ldap',
                    value: ''
                },
                {
                    name: 'ldap.host',
                    value: ''
                },
                {
                    name: 'ldap.port',
                    value: ''
                },
                {
                    name: 'ldap.context',
                    value: ''
                },
                {
                    name: 'ldap.user',
                    value: ''
                }
            ]
        }
    },
    methods: {
        loadProps() {
            this.init = true;
            for(let i = 0; i < this.props.length; i++) {
                axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/prop/${this.props[i].name}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                    this.props[i].value = r.data;
                }).catch(function(err) {
                    console.log(err);
                    this.props[i].value = 'undefined';
                });
            }
        }
    }
};
</script>
