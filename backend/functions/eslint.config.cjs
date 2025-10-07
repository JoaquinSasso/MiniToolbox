// functions/eslint.config.cjs
const tseslint = require("@typescript-eslint/eslint-plugin");
const tsparser = require("@typescript-eslint/parser");

/** @type {import('eslint').Linter.FlatConfig[]} */
module.exports = [
	{
		files: ["**/*.ts"],
		ignores: ["lib/**", "node_modules/**"],
		languageOptions: {
			parser: tsparser,
			ecmaVersion: "latest",
			sourceType: "module",
			// No ponemos "project" para evitar ruido con TS program
		},
		plugins: {
			"@typescript-eslint": tseslint,
		},
		rules: {
			// Reglas razonables para Functions
			"no-console": "off",
			"@typescript-eslint/no-unused-vars": [
				"warn",
				{ argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
			],
			"@typescript-eslint/no-explicit-any": "off",
			"@typescript-eslint/ban-ts-comment": "off",
		},
	},
];
