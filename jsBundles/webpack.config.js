const path = require('path');
const nodeModules = path.resolve(__dirname, '../node_modules');

module.exports = {

  entry: {
    'fake-indexeddb': './src/fake-indexeddb.js',
  },

  target: 'node',

  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: '[name].js',
    // library: '',
    libraryTarget: 'global',
  },

  resolve: {
    modules: [
      nodeModules,
      'node_modules',
    ],
  },
  resolveLoader: {
    modules: [
      nodeModules,
    ],
  },

  // Using 'production' here breaks fake-indexeddb somehow.
  // Running the code in https://github.com/dumbmatter/fakeIndexedDB#use from Scala.JS prints:
  //
  //     From index: { title: 'Quarry Memories', author: 'Fred', isbn: 123456 }
  //     From cursor: undefined
  //     From cursor: undefined
  //     All done!
  //
  // instead of the expected
  //
  //    From index: { title: 'Quarry Memories', author: 'Fred', isbn: 123456 }
  //    From cursor: { title: 'Water Buffaloes', author: 'Fred', isbn: 234567 }
  //    From cursor: { title: 'Bedrock Nights', author: 'Barney', isbn: 345678 }
  //    All done!
  //
  mode: 'development',

  performance: {
    hints: false
  },

  bail: true,
};
