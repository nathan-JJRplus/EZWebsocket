/* eslint-env node */

const base = require("@mendix/pluggable-widgets-tools/configs/eslint.ts.base.json");

module.exports = {
    ...base,
    parserOptions: {
        ...base.parserOptions,
        project: "./tsconfig.json",
        tsconfigRootDir: __dirname
    }
};
