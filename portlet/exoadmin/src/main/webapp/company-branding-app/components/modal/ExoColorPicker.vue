<template>
  <v-dialog
    ref="dialog"
    v-model="modal"
    color="white"
    width="290px">
    <template v-slot:activator="{ on }">
      <v-text-field
        v-model="value"
        :label="label"
        readonly
        v-on="on" />
    </template>
    <v-color-picker
      v-model="value"
      :swatches="swatches"
      mode="hexa"
      show-swatches />
    <v-row class="mx-0 white">
      <v-col class="center">
        <v-btn text color="primary" @click="cancel">Cancel</v-btn>
      </v-col>
      <v-col class="center">
        <v-btn text color="primary" @click="save">OK</v-btn>
      </v-col>
    </v-row>
  </v-dialog>
</template>
<script>
export default {
  props: {
    label: {
      type: String,
      default: function() {
        return null;
      },
    },
    value: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data: () => ({
    modal: false,
    originalValue: null,
    swatches: [
      ['#FF0000', '#AA0000', '#550000'],
      ['#FFFF00', '#AAAA00', '#555500'],
      ['#00FF00', '#00AA00', '#005500'],
      ['#00FFFF', '#00AAAA', '#005555'],
      ['#0000FF', '#0000AA', '#000055'],
    ],
  }),
  watch: {
    modal() {
      if (this.modal) {
        this.originalValue = this.value;
      }
    }
  },
  methods: {
    cancel() {
      this.value = this.originalValue;
      this.modal = false;
    },
    save() {
      this.$emit('input', this.value);
      this.modal = false;
    }
  }
};
</script>
