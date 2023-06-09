import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Swal from "sweetalert2";
import axios from 'axios';
import GetMySentimentPc from './GetMySentimentPc';
import GetMySentimentMobile from './GetMySentimentMobile';
import GetMySentimentTablet from './GetMySentimentTablet';

const GetMySentiment = (props) => {

    const isPc = props.isPc;
    const isMobile = props.isMobile;
    const isTablet = props.isTablet;

    const navigate = useNavigate();

    const [inputCount, setInputCount] = useState(0);

    const [text, setText] = useState('');

    const onInput = (e) => {
        setInputCount(e.target.value.length);
        setText(e.target.value);
    }

    const checkEng = (text) => {
        var Regex = /[a-zA-Z\u4E00-\u9FFF\u3040-\u309F\u30A0-\u30FF\u0600-\u06FF]+/; //한글, 숫자, 특수문자를 제외한 다른 문자 입력 방지
        return Regex.test(text);
    }

    useEffect(() => {
        if (checkEng(text)) {
            Swal.fire({
                icon: 'warning',
                text: '한글만 입력해주세요.',
                showCancelButton: false,
                confirmButtonText: "확인",
                confirmButtonColor: '#1564A8'
            })
        }
    }, [text])

    const getSentiment = () => {
        if (text === '') {
            Swal.fire({
                icon: 'warning',
                text: '분석할 문장을 입력해주세요.',
                showCancelButton: false,
                confirmButtonText: "확인",
                confirmButtonColor: '#1564A8'
            })
        } else if (checkEng(text)) {
            Swal.fire({
                icon: 'warning',
                text: '한글만 입력해주세요.',
                showCancelButton: false,
                confirmButtonText: "확인",
                confirmButtonColor: '#1564A8'
            })
        } else {
            axios.get(`/GetMySentiment?text=${text}`)
                .then((res) => {
                    navigate('/MySentimentResult', { state: { result: res.data } });
                })
                .catch((error) => console.log(error))
        }
    }

    return (
        <div style={{ 'textAlign': 'center', background: `#FAFBFC` }}>
            {isPc && <GetMySentimentPc getSentiment={getSentiment} onInput={onInput} inputCount={inputCount} />}
            {isMobile && <GetMySentimentMobile getSentiment={getSentiment} onInput={onInput} inputCount={inputCount} />}
            {isTablet && <GetMySentimentTablet getSentiment={getSentiment} onInput={onInput} inputCount={inputCount} />}
        </div>
    );
};

export default GetMySentiment;