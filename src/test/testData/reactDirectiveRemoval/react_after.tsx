

import { useState } from "react";

export default function ReactDirectiveRemoval() {
    const [state, _] = useState(0);
    return <div>{
        state === 0 ? <div>0</div> : <div>1</div>
    }</div>;
}
