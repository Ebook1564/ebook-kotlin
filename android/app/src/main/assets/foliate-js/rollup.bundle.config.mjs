import { nodeResolve } from '@rollup/plugin-node-resolve'
import terser from '@rollup/plugin-terser'

export default {
    input: 'view.js',
    output: {
        file: 'bundle.js',
        format: 'iife',
        name: 'FoliateBundle',
        sourcemap: false,
        inlineDynamicImports: true
    },
    plugins: [
        nodeResolve({
            browser: true,
            preferBuiltins: false
        }),
        terser()
    ]
}
